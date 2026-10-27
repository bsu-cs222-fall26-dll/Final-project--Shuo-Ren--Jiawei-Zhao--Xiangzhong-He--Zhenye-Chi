package compass.canvas;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CanvasConnectionTest {

    private static final String SCHOOL =
            "https://school.example";

    private static CanvasApiService.Response ok(String body) {
        return new CanvasApiService.Response(
                200,
                body,
                List.of()
        );
    }

    private static CanvasApiService service(
            CanvasApiService.Transport transport
    ) {
        return new CanvasApiService(
                SCHOOL,
                "test-token",
                transport
        );
    }

    @Test
    void checksProfileAndCountsUniqueAccessibleCoursesAcrossPages()
            throws Exception {

        List<HttpRequest> requests = new ArrayList<>();

        var api = service(request -> {
            requests.add(request);

            assertEquals("GET", request.method());

            assertEquals(
                    "Bearer test-token",
                    request.headers()
                            .firstValue("Authorization")
                            .orElseThrow()
            );

            String path = request.uri().getPath();

            if (path.equals("/api/v1/users/self")) {
                return ok(
                        "{\"id\":42,\"name\":\"Student Example\"}"
                );
            }

            assertEquals("/api/v1/courses", path);

            String query = request.uri().getQuery();

            if (query.contains("page=2")) {
                return ok("[{\"id\":1},{\"id\":2}]");
            }

            assertTrue(query.contains("enrollment_type=student"));
            assertTrue(query.contains("include[]=term"));
            assertTrue(query.contains("include[]=concluded"));

            return new CanvasApiService.Response(
                    200,
                    "[{\"id\":1}]",
                    List.of(
                            "<" + SCHOOL
                                    + "/api/v1/courses?page=2>; rel=\"next\""
                    )
            );
        });

        var result = api.checkConnection();

        assertEquals("Student Example", result.name());
        assertEquals(42, result.userId());
        assertEquals(2, result.courseCount());
        assertEquals(3, requests.size());
    }

    @Test
    void readsNamesTermsAndFallbacks() throws Exception {
        var courses = service(request -> ok("""
                [{"id":1,"name":"  CS222  ","term":{"name":"Fall 2026"}},
                 {"id":2,"term":null},
                 {"id":3,"name":" ","term":{"name":" "}}]
                """)).fetchCourses();

        assertEquals(
                new CanvasCourse(1, "CS222", "Fall 2026", false),
                courses.get(0)
        );

        assertEquals("Course 2", courses.get(1).name());
        assertEquals("Term not provided", courses.get(1).term());
        assertEquals("Course 3", courses.get(2).name());
        assertEquals("Term not provided", courses.get(2).term());
    }

    @Test
    void recognizesConcludedFlagAndCompletedWorkflow()
            throws Exception {

        var courses = service(request -> ok("""
                [{"id":1,"concluded":true},
                 {"id":2,"workflow_state":"completed"},
                 {"id":3,"concluded":false,"workflow_state":"available"}]
                """)).fetchCourses();

        assertTrue(courses.get(0).concluded());
        assertTrue(courses.get(1).concluded());
        assertFalse(courses.get(2).concluded());
    }

    @Test
    void returnsEmptyAndImmutableCourseLists()
            throws Exception {

        assertTrue(
                service(request -> ok("[]"))
                        .fetchCourses()
                        .isEmpty()
        );

        var courses = service(
                request -> ok("[{\"id\":1}]")
        ).fetchCourses();

        assertThrows(
                UnsupportedOperationException.class,
                courses::clear
        );
    }

    @Test
    void rejectsInvalidProfilesBeforeRequestingCourses() {
        for (String profile : List.of(
                "null",
                "[]",
                "{}",
                "{\"id\":0}",
                "{\"id\":1.5}",
                "{\"id\":\"42\"}",
                "{\"id\":9223372036854775808}"
        )) {
            AtomicInteger calls = new AtomicInteger();

            var api = service(request -> {
                calls.incrementAndGet();

                assertEquals(
                        "/api/v1/users/self",
                        request.uri().getPath()
                );

                return ok(profile);
            });

            assertThrows(IOException.class, api::checkConnection);
            assertEquals(1, calls.get());
        }
    }

    @Test
    void suppliesProfileNameFallback() throws Exception {
        var result = service(
                request -> request.uri().getPath().endsWith("/self")
                        ? ok("{\"id\":42,\"name\":null}")
                        : ok("[]")
        ).checkConnection();

        assertEquals("Canvas user", result.name());
        assertEquals(0, result.courseCount());
    }

    @Test
    void rejectsInvalidCourseIdsInsteadOfSilentlyReturningPartialResults() {
        for (String id : List.of(
                "null",
                "0",
                "-1",
                "1.5",
                "\"1\"",
                "9223372036854775808"
        )) {
            var api = service(
                    request -> ok(
                            "[{\"id\":1},{\"id\":" + id + "}]"
                    )
            );

            assertThrows(IOException.class, api::fetchCourses);
        }
    }

    @Test
    void rejectsMalformedCourseFields() {
        for (String body : List.of(
                "{}",
                "null",
                "[1]",
                "[{}]",
                "[{\"id\":1,\"term\":[]}]",
                "[{\"id\":1,\"name\":true}]",
                "[{\"id\":1,\"concluded\":\"false\"}]"
        )) {
            var api = service(request -> ok(body));

            assertThrows(IOException.class, api::fetchCourses);
        }
    }

    @Test
    void reportsHttpFailuresWithoutEchoingResponseBodiesOrFollowingRedirects() {
        for (int status : List.of(401, 403, 404, 500, 302)) {
            AtomicInteger calls = new AtomicInteger();

            var api = service(request -> {
                calls.incrementAndGet();

                return new CanvasApiService.Response(
                        status,
                        "secret-test-token",
                        List.of()
                );
            });

            IOException error = assertThrows(
                    IOException.class,
                    api::checkConnection
            );

            assertTrue(
                    error.getMessage().contains(
                            Integer.toString(status)
                    )
            );

            assertFalse(
                    error.toString().contains("secret-test-token")
            );

            assertNull(error.getCause());
            assertEquals(1, calls.get());
        }
    }

    @Test
    void reportsInvalidJsonWithoutParserDetails() {
        var api = service(
                request -> ok("{secret-test-token: [")
        );

        IOException error = assertThrows(
                IOException.class,
                api::checkConnection
        );

        assertTrue(error.getMessage().contains("JSON"));
        assertFalse(error.toString().contains("secret-test-token"));
        assertNull(error.getCause());
    }

    @Test
    void reportsEmptyResponses() {
        for (String body : new String[]{null, "", "   "}) {
            var api = service(request -> ok(body));

            assertThrows(
                    IOException.class,
                    api::checkConnection
            );
        }
    }

    @Test
    void sanitizesTimeoutAndNetworkFailureMessages() {
        for (IOException failure : List.of(
                new HttpTimeoutException("secret-test-token"),
                new IOException("secret-test-token")
        )) {
            var api = service(request -> {
                throw failure;
            });

            IOException error = assertThrows(
                    IOException.class,
                    api::checkConnection
            );

            assertFalse(
                    error.toString().contains("secret-test-token")
            );

            assertNull(error.getCause());
        }
    }

    @Test
    void propagatesInterruption() {
        var api = service(request -> {
            throw new InterruptedException("cancelled");
        });

        assertThrows(
                InterruptedException.class,
                api::fetchCourses
        );
    }

    @Test
    void readsNextWithOtherRelationsAndParameters()
            throws Exception {

        AtomicInteger calls = new AtomicInteger();

        var courses = service(request -> {
            if (calls.incrementAndGet() == 2) {
                return ok("[{\"id\":2}]");
            }

            return new CanvasApiService.Response(
                    200,
                    "[{\"id\":1}]",
                    List.of(
                            "<" + SCHOOL
                                    + "/api/v1/courses?page=1>; rel=\"current\", "
                                    + "<" + SCHOOL
                                    + ":443/api/v1/courses?opaque=a,b>; "
                                    + "title=\"second\"; rel=next"
                    )
            );
        }).fetchCourses();

        assertEquals(2, courses.size());
        assertEquals(2, calls.get());
    }

    @Test
    void blocksUntrustedPaginationBeforeSendingAnotherRequest() {
        for (String next : List.of(
                "https://other.example/api/v1/courses?page=2",
                "http://school.example/api/v1/courses?page=2",
                "https://school.example:8443/api/v1/courses?page=2",
                "https://user@school.example/api/v1/courses?page=2",
                "https://school.example/login",
                "https://school.example/api/v1/courses#fragment"
        )) {
            AtomicInteger calls = new AtomicInteger();

            var api = service(request -> {
                calls.incrementAndGet();

                return new CanvasApiService.Response(
                        200,
                        "[]",
                        List.of("<" + next + ">; rel=\"next\"")
                );
            });

            assertThrows(IOException.class, api::fetchCourses);
            assertEquals(1, calls.get());
        }
    }

    @Test
    void rejectsMalformedPaginationInsteadOfReturningATruncatedList() {
        var api = service(
                request -> new CanvasApiService.Response(
                        200,
                        "[]",
                        List.of("broken; rel=next")
                )
        );

        assertThrows(IOException.class, api::fetchCourses);
    }

    @Test
    void stopsPaginationCyclesAndLimitsPageCount() {
        AtomicInteger cycleCalls = new AtomicInteger();

        var cycleApi = service(request -> {
            cycleCalls.incrementAndGet();

            return new CanvasApiService.Response(
                    200,
                    "[]",
                    List.of("<" + request.uri() + ">; rel=next")
            );
        });

        assertThrows(IOException.class, cycleApi::fetchCourses);
        assertEquals(1, cycleCalls.get());

        AtomicInteger pageCalls = new AtomicInteger();

        var endlessApi = service(request -> {
            int page = pageCalls.incrementAndGet();

            return new CanvasApiService.Response(
                    200,
                    "[]",
                    List.of(
                            "<" + SCHOOL
                                    + "/api/v1/courses?page="
                                    + (page + 1)
                                    + ">; rel=next"
                    )
            );
        });

        assertThrows(IOException.class, endlessApi::fetchCourses);
        assertEquals(100, pageCalls.get());
    }
}
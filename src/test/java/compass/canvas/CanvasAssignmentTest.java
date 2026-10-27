package compass.canvas;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CanvasAssignmentTest {

    private static final String SCHOOL = "https://school.example";

    private static final CanvasCourse COURSE =
            new CanvasCourse(10, "CS222", "Fall 2026", false);

    private static CanvasApiService api(
            CanvasApiService.Transport transport
    ) {
        return new CanvasApiService(
                SCHOOL,
                "test-token",
                transport
        );
    }

    private static CanvasApiService.Response ok(String body) {
        return new CanvasApiService.Response(
                200,
                body,
                List.of()
        );
    }

    @Test
    void requestsOnlySelectedCoursesAndPreservesAssignmentDetails()
            throws Exception {

        List<String> requests = new ArrayList<>();

        var result = api(request -> {
            requests.add(request.uri().getPath());

            assertEquals(
                    "/api/v1/courses/10/assignments",
                    request.uri().getPath()
            );

            assertEquals(
                    "Bearer test-token",
                    request.headers()
                            .firstValue("Authorization")
                            .orElseThrow()
            );

            assertEquals("GET", request.method());

            return ok("""
                    [{"id":7,"course_id":10,"name":" Project Plan ",
                      "due_at":"2026-10-27T01:00:00Z","points_possible":12.5}]
                    """);
        }).fetchAssignmentsForCourses(List.of(COURSE));

        assertEquals(1, requests.size());
        assertEquals(1, result.size());

        var item = result.get(0);

        assertEquals(7, item.canvasAssignmentId());
        assertEquals(10, item.courseId());
        assertEquals("CS222", item.courseName());
        assertEquals("Project Plan", item.name());
        assertEquals(new BigDecimal("12.5"), item.pointsPossible());

        assertEquals(
                OffsetDateTime.parse("2026-10-27T01:00:00Z")
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime(),
                item.dueDate()
        );
    }

    @Test
    void emptySelectionMakesNoRequests() throws Exception {
        var result = api(request -> {
            throw new AssertionError("No requests expected");
        }).fetchAssignmentsForCourses(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void validatesAllSelectedCoursesBeforeMakingRequests() {
        var service = api(request -> {
            throw new AssertionError("No requests expected");
        });

        assertThrows(
                IllegalArgumentException.class,
                () -> service.fetchAssignmentsForCourses(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.fetchAssignmentsForCourses(
                        Arrays.asList(COURSE, null)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.fetchAssignmentsForCourses(
                        List.of(
                                COURSE,
                                new CanvasCourse(-1, "Invalid", "", false)
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.fetchAssignmentsForCourses(
                        List.of(new CanvasCourse(1, " ", "", false))
                )
        );
    }

    @Test
    void readsMultipleCoursesAndAvoidsDuplicateCourseRequests()
            throws Exception {

        AtomicInteger calls = new AtomicInteger();

        var result = api(request -> {
            calls.incrementAndGet();

            assertTrue(
                    List.of(
                            "/api/v1/courses/10/assignments",
                            "/api/v1/courses/20/assignments"
                    ).contains(request.uri().getPath())
            );

            return ok("[{\"id\":7}]");
        }).fetchAssignmentsForCourses(
                List.of(
                        COURSE,
                        COURSE,
                        new CanvasCourse(20, "Math", "", false)
                )
        );

        assertEquals(2, calls.get());
        assertEquals(2, result.size());
        assertEquals("Math", result.get(1).courseName());
    }

    @Test
    void readsAllAssignmentPagesAndDeduplicatesRepeatedIds()
            throws Exception {

        AtomicInteger calls = new AtomicInteger();

        var result = api(request -> {
            if (calls.incrementAndGet() == 2) {
                return ok(
                        "[{\"id\":7,\"name\":\"Updated\"},{\"id\":8}]"
                );
            }

            return new CanvasApiService.Response(
                    200,
                    "[{\"id\":7,\"name\":\"Original\"}]",
                    List.of(
                            "<" + SCHOOL
                                    + "/api/v1/courses/10/assignments?page=2>; rel=next"
                    )
            );
        }).fetchAssignmentsForCourses(List.of(COURSE));

        assertEquals(2, calls.get());
        assertEquals(2, result.size());
        assertEquals("Updated", result.get(0).name());
        assertEquals(8, result.get(1).canvasAssignmentId());

        assertThrows(
                UnsupportedOperationException.class,
                result::clear
        );
    }

    @Test
    void leavesMissingDatesNullAndMissingPointsZero()
            throws Exception {

        var result = api(
                request -> ok(
                        "[{\"id\":7},"
                                + "{\"id\":8,\"due_at\":null,\"points_possible\":null}]"
                )
        ).fetchAssignmentsForCourses(List.of(COURSE));

        for (var assignment : result) {
            assertNull(assignment.dueDate());
            assertEquals(
                    BigDecimal.ZERO,
                    assignment.pointsPossible()
            );
            assertEquals(
                    "Untitled assignment",
                    assignment.name()
            );
        }
    }

    @Test
    void convertsOffsetsToLocalTimeWithoutInventingDates()
            throws Exception {

        var service = api(
                request -> ok(
                        "[{\"id\":7,\"due_at\":\"2026-10-27T01:00:00Z\"},"
                                + "{\"id\":8,\"due_at\":\"2026-10-27T09:00:00+08:00\"}]"
                )
        );

        var result = new CanvasAssignmentReader(
                service,
                ZoneId.of("America/Indianapolis")
        ).fetch(List.of(COURSE));

        assertEquals(
                LocalDateTime.of(2026, 10, 26, 21, 0),
                result.get(0).dueDate()
        );

        assertEquals(
                result.get(0).dueDate(),
                result.get(1).dueDate()
        );
    }

    @Test
    void rejectsInvalidIdsAndMismatchedCourseIds() {
        for (String body : List.of(
                "[{}]",
                "[{\"id\":0}]",
                "[{\"id\":1.5}]",
                "[{\"id\":\"7\"}]",
                "[{\"id\":9223372036854775808}]",
                "[{\"id\":7,\"course_id\":99}]"
        )) {
            var service = api(request -> ok(body));

            assertThrows(
                    IOException.class,
                    () -> service.fetchAssignmentsForCourses(
                            List.of(COURSE)
                    )
            );
        }
    }

    @Test
    void rejectsInvalidDatesPointsAndNamesWithoutEchoingInput() {
        for (String field : List.of(
                "\"due_at\":\"secret-marker\"",
                "\"due_at\":12",
                "\"points_possible\":-1",
                "\"points_possible\":\"secret-marker\"",
                "\"name\":[]"
        )) {
            var service = api(
                    request -> ok("[{\"id\":7," + field + "}]")
            );

            IOException error = assertThrows(
                    IOException.class,
                    () -> service.fetchAssignmentsForCourses(
                            List.of(COURSE)
                    )
            );

            assertFalse(
                    error.toString().contains("secret-marker")
            );
            assertNull(error.getCause());
        }
    }

    @Test
    void rejectsUnexpectedAssignmentResponseShapes() {
        for (String body : List.of(
                "{}",
                "null",
                "[null]",
                "[1]"
        )) {
            var service = api(request -> ok(body));

            assertThrows(
                    IOException.class,
                    () -> service.fetchAssignmentsForCourses(
                            List.of(COURSE)
                    )
            );
        }
    }

    @Test
    void blocksAssignmentPaginationToOtherServers() {
        AtomicInteger calls = new AtomicInteger();

        var service = api(request -> {
            calls.incrementAndGet();

            return new CanvasApiService.Response(
                    200,
                    "[]",
                    List.of(
                            "<https://other.example/api/v1/courses/10/assignments?page=2>; rel=next"
                    )
            );
        });

        assertThrows(
                IOException.class,
                () -> service.fetchAssignmentsForCourses(
                        List.of(COURSE)
                )
        );

        assertEquals(1, calls.get());
    }

    @Test
    void blocksPaginationToUnselectedCoursesOrDifferentEndpoints() {
        for (String next : List.of(
                "/api/v1/courses/99/assignments?page=2",
                "/api/v1/courses?page=2"
        )) {
            AtomicInteger calls = new AtomicInteger();

            var service = api(request -> {
                calls.incrementAndGet();

                return new CanvasApiService.Response(
                        200,
                        "[]",
                        List.of(
                                "<" + SCHOOL + next + ">; rel=next"
                        )
                );
            });

            assertThrows(
                    IOException.class,
                    () -> service.fetchAssignmentsForCourses(
                            List.of(COURSE)
                    )
            );

            assertEquals(1, calls.get());
        }
    }

    @Test
    void propagatesLaterCourseFailureInsteadOfReturningPartialData() {
        var service = api(
                request -> request.uri().getPath().contains("/10/")
                        ? ok("[{\"id\":7}]")
                        : new CanvasApiService.Response(
                        403,
                        "denied",
                        List.of()
                )
        );

        IOException error = assertThrows(
                IOException.class,
                () -> service.fetchAssignmentsForCourses(
                        List.of(
                                COURSE,
                                new CanvasCourse(20, "Math", "", false)
                        )
                )
        );

        assertTrue(error.getMessage().contains("403"));
    }

    @Test
    void propagatesInterruptionDuringAssignmentFetch() {
        var service = api(request -> {
            throw new InterruptedException("cancelled");
        });

        assertThrows(
                InterruptedException.class,
                () -> service.fetchAssignmentsForCourses(
                        List.of(COURSE)
                )
        );
    }
}
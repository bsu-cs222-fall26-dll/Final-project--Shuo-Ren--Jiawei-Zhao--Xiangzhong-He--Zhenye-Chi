package compass.canvas;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CanvasApiServiceTest {
    @Test
    void normalizesSchoolCourseAndAssignmentPages() {
        for (String page : List.of("", "/", "/courses/123",
                "/courses/123/assignments/456?view=student#details")) {
            var service = new CanvasApiService("  https://school.example" + page + "  ", "test-token");
            assertEquals(URI.create("https://school.example/api/v1/users/self"),
                    service.createGetRequest("api/v1/users/self").uri());
        }
    }

    @Test
    void putsBearerTokenOnlyInHeaderAndBuildsReadOnlyRequest() {
        var request = new CanvasApiService("https://school.example", "  4~test-token_123==  ")
                .createGetRequest("api/v1/courses?enrollment_type=student");
        assertEquals("Bearer 4~test-token_123==", request.headers().firstValue("Authorization").orElseThrow());
        assertEquals("application/json", request.headers().firstValue("Accept").orElseThrow());
        assertEquals("GET", request.method());
        assertTrue(request.bodyPublisher().isEmpty());
        assertFalse(request.uri().toString().contains("test-token"));
        assertEquals(Duration.ofSeconds(20), request.timeout().orElseThrow());
    }

    @Test
    void preservesCustomHttpsPortAndNormalizesScheme() {
        var request = new CanvasApiService("HTTPS://school.example:8443/courses/123", "test-token")
                .createGetRequest("api/v1/courses");
        assertEquals(URI.create("https://school.example:8443/api/v1/courses"), request.uri());
    }

    @Test
    void rejectsInsecureUrlsCredentialsAndInvalidPorts() {
        for (String url : List.of("http://school.example", "ftp://school.example",
                "https://username:password@school.example", "https://school.example:0",
                "https://school.example:65536")) {
            assertThrows(IllegalArgumentException.class, () -> new CanvasApiService(url, "test-token"));
        }
    }

    @Test
    void rejectsMissingAndMalformedUrlsWithoutEchoingInput() {
        for (String url : new String[]{null, "", "  ", "school.example", "https://",
                "https://school.example/invalid path?secret-marker"}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> new CanvasApiService(url, "test-token"));
            assertFalse(error.toString().contains("secret-marker"));
            assertNull(error.getCause());
        }
    }

    @Test
    void rejectsInvalidTokensWithoutRepeatingThem() {
        for (String token : new String[]{null, "", "  ", "secret-marker\nvalue",
                "secret-marker\rvalue", "secret-marker\tvalue", "secret-marker value",
                "secret-marker\u0000value", "secret-marker\u4e2dvalue", "secret-marker=middle"}) {
            var error = assertThrows(IllegalArgumentException.class,
                    () -> new CanvasApiService("https://school.example", token));
            assertFalse(error.toString().contains("secret-marker"));
            assertNull(error.getCause());
        }
    }

    @Test
    void rejectsAbsoluteAndNonApiTargetsBeforeAttachingCredentials() {
        var service = new CanvasApiService("https://school.example", "test-token");
        for (String path : new String[]{null, "", "https://other.example/api/v1/courses",
                "//other.example/api/v1/courses", "/profile", "api/v1/invalid path"}) {
            assertThrows(IllegalArgumentException.class, () -> service.createGetRequest(path));
        }
    }
}

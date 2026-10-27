package compass.canvas;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;

/** Task 11: school URL normalization and read-only Bearer requests. */
public final class CanvasApiService {
    private final URI baseUrl;
    private final String token;

    public CanvasApiService(String canvasUrl, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("A Canvas token is required.");
        }
        String trimmedToken = token.trim();
        // Validate before HttpRequest can include invalid header input in an error.
        if (!trimmedToken.matches("[A-Za-z0-9._~+/-]+=*")) {
            throw new IllegalArgumentException("Enter a valid Canvas token without spaces or control characters.");
        }
        this.baseUrl = normalizeUrl(canvasUrl);
        this.token = trimmedToken;
    }

    private static URI normalizeUrl(String canvasUrl) {
        if (canvasUrl == null || canvasUrl.isBlank()) {
            throw new IllegalArgumentException("Enter your school's Canvas URL.");
        }
        try {
            URI entered = URI.create(canvasUrl.trim());
            int port = entered.getPort();
            if (!"https".equalsIgnoreCase(entered.getScheme())
                    || entered.getHost() == null
                    || entered.getRawUserInfo() != null
                    || port == 0 || port > 65535) {
                throw new IllegalArgumentException();
            }
            return new URI("https", null, entered.getHost(), port, "/", null, null);
        } catch (IllegalArgumentException | URISyntaxException exception) {
            // Never repeat the supplied URL or attach a cause containing it.
            throw new IllegalArgumentException("Enter a valid HTTPS Canvas URL with a port from 1 to 65535 if specified.");
        }
    }

    // Package-private: service methods and tests can use it without exposing a token-bearing request to the UI.
    HttpRequest createGetRequest(String apiPath) {
        if (apiPath == null || !apiPath.startsWith("api/v1/")) {
            throw new IllegalArgumentException("Use a relative Canvas API path beginning with api/v1/.");
        }
        URI target;
        try {
            target = baseUrl.resolve(apiPath);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Canvas API path.");
        }
        return HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
    }
}

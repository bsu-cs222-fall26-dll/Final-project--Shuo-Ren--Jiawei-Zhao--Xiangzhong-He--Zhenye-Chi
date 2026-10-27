package compass.canvas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/** Read-only Canvas connection and course discovery. */
public final class CanvasApiService {

    public record ConnectionCheck(
            String name,
            long userId,
            int courseCount
    ) { }

    @FunctionalInterface
    interface Transport {
        Response send(HttpRequest request)
                throws IOException, InterruptedException;
    }

    record Response(
            int statusCode,
            String body,
            List<String> links
    ) { }

    private static final int MAX_PAGES = 100;

    private static final String COURSES_PATH =
            "api/v1/courses?enrollment_type=student"
                    + "&per_page=100"
                    + "&include%5B%5D=term"
                    + "&include%5B%5D=concluded";

    private static final Pattern RELATION = Pattern.compile(
            "(?:^|;)\\s*rel\\s*=\\s*(?:\"([^\"]*)\"|([^;\\s]+))",
            Pattern.CASE_INSENSITIVE
    );

    private final URI baseUrl;
    private final String token;
    private final Transport transport;

    public CanvasApiService(String canvasUrl, String token) {
        this(canvasUrl, token, defaultTransport());
    }

    CanvasApiService(
            String canvasUrl,
            String token,
            Transport transport
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "A Canvas token is required."
            );
        }

        String trimmedToken = token.trim();

        if (!trimmedToken.matches("[A-Za-z0-9._~+/-]+=*")) {
            throw new IllegalArgumentException(
                    "Enter a valid Canvas token without spaces or control characters."
            );
        }

        this.baseUrl = normalizeUrl(canvasUrl);
        this.token = trimmedToken;
        this.transport = java.util.Objects.requireNonNull(
                transport,
                "transport"
        );
    }

    private static Transport defaultTransport() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        return request -> {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return new Response(
                    response.statusCode(),
                    response.body(),
                    response.headers().allValues("Link")
            );
        };
    }

    public ConnectionCheck checkConnection()
            throws IOException, InterruptedException {

        JsonElement profile = parseJson(
                send(createGetRequest("api/v1/users/self")).body()
        );

        if (!profile.isJsonObject()) {
            throw new IOException(
                    "Canvas did not return a valid user profile."
            );
        }

        JsonObject user = profile.getAsJsonObject();
        long id = positiveId(user.get("id"));

        String name = optionalText(
                user.get("name"),
                "Canvas user"
        );

        return new ConnectionCheck(
                name,
                id,
                fetchCourses().size()
        );
    }

    public List<CanvasCourse> fetchCourses()
            throws IOException, InterruptedException {

        LinkedHashMap<Long, CanvasCourse> courses =
                new LinkedHashMap<>();

        for (JsonElement element : getPages(COURSES_PATH)) {
            if (!element.isJsonObject()) {
                throw new IOException(
                        "Canvas returned an invalid course record."
                );
            }

            JsonObject course = element.getAsJsonObject();
            long id = positiveId(course.get("id"));

            String name = optionalText(
                    course.get("name"),
                    "Course " + id
            );

            JsonElement termValue = course.get("term");
            String term = "Term not provided";

            if (termValue != null && !termValue.isJsonNull()) {
                if (!termValue.isJsonObject()) {
                    throw new IOException(
                            "Canvas returned an invalid course term."
                    );
                }

                term = optionalText(
                        termValue.getAsJsonObject().get("name"),
                        term
                );
            }

            boolean concluded =
                    optionalBoolean(course.get("concluded"))
                            || "completed".equals(
                            optionalText(
                                    course.get("workflow_state"),
                                    ""
                            )
                    );

            courses.put(
                    id,
                    new CanvasCourse(id, name, term, concluded)
            );
        }

        return List.copyOf(courses.values());
    }

    public List<CanvasAssignment> fetchAssignmentsForCourses(
            List<CanvasCourse> selectedCourses
    ) throws IOException, InterruptedException {
        return new CanvasAssignmentReader(this).fetch(selectedCourses);
    }

    List<JsonElement> getPages(String apiPath)
            throws IOException, InterruptedException {

        List<JsonElement> result = new ArrayList<>();
        Set<URI> visited = new HashSet<>();

        URI next = createGetRequest(apiPath).uri();
        URI requested = next;

        while (next != null) {
            if (visited.size() >= MAX_PAGES
                    || !visited.add(next)) {
                throw new IOException(
                        "Canvas pagination repeated a page or exceeded the safety limit."
                );
            }

            requireSameServer(next);

            if (!next.getPath().equals(requested.getPath())) {
                throw new IOException(
                        "Canvas pagination changed the requested endpoint."
                );
            }

            Response response = send(buildRequest(next));
            JsonElement page = parseJson(response.body());

            if (!page.isJsonArray()) {
                throw new IOException(
                        "Canvas returned an unexpected list."
                );
            }

            page.getAsJsonArray().forEach(result::add);
            next = nextPage(response.links());
        }

        return result;
    }

    private Response send(HttpRequest request)
            throws IOException, InterruptedException {

        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException(
                    "Canvas request cancelled."
            );
        }

        Response response;

        try {
            response = transport.send(request);
        } catch (HttpTimeoutException exception) {
            throw new IOException(
                    "Canvas request timed out. Try again."
            );
        } catch (IOException exception) {
            throw new IOException(
                    "Could not connect to Canvas. Check the network and school URL."
            );
        }

        switch (response.statusCode()) {
            case 200:
                return response;

            case 401:
                throw new IOException(
                        "Canvas rejected this token (HTTP 401). Check the URL and token expiration."
                );

            case 403:
                throw new IOException(
                        "Canvas denied access (HTTP 403). Check account permissions."
                );

            case 404:
                throw new IOException(
                        "Canvas API was not found (HTTP 404). Check the school URL."
                );

            default:
                throw new IOException(
                        "Canvas request failed (HTTP "
                                + response.statusCode()
                                + ")."
                );
        }
    }

    private static JsonElement parseJson(String body)
            throws IOException {

        if (body == null || body.isBlank()) {
            throw new IOException(
                    "Canvas returned an empty response."
            );
        }

        try {
            return JsonParser.parseString(body);
        } catch (JsonParseException | IllegalStateException exception) {
            throw new IOException(
                    "Canvas returned invalid JSON."
            );
        }
    }

    private static long positiveId(JsonElement value)
            throws IOException {

        try {
            if (value == null
                    || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException();
            }

            long id = value.getAsBigDecimal().longValueExact();

            if (id <= 0) {
                throw new IllegalArgumentException();
            }

            return id;
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new IOException(
                    "Canvas returned a missing or invalid ID."
            );
        }
    }

    private static String optionalText(
            JsonElement value,
            String fallback
    ) throws IOException {

        if (value == null || value.isJsonNull()) {
            return fallback;
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            throw new IOException(
                    "Canvas returned an invalid text field."
            );
        }

        String text = value.getAsString().trim();

        return text.isBlank() ? fallback : text;
    }

    private static boolean optionalBoolean(JsonElement value)
            throws IOException {

        if (value == null || value.isJsonNull()) {
            return false;
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IOException(
                    "Canvas returned an invalid course status."
            );
        }

        return value.getAsBoolean();
    }

    private URI nextPage(List<String> linkHeaders)
            throws IOException {

        URI next = null;

        for (String header : linkHeaders) {
            for (String part : header.split(",(?=\\s*<)")) {
                int end = part.indexOf('>');

                String parameters = end >= 0
                        ? part.substring(end + 1)
                        : part;

                var relation = RELATION.matcher(parameters);
                boolean isNext = false;

                while (relation.find()) {
                    String value = relation.group(1) != null
                            ? relation.group(1)
                            : relation.group(2);

                    if (List.of(value.split("\\s+")).contains("next")) {
                        isNext = true;
                    }
                }

                if (!isNext) {
                    continue;
                }

                int start = part.indexOf('<');

                if (next != null || start < 0 || end <= start) {
                    throw new IOException(
                            "Canvas returned an invalid pagination link."
                    );
                }

                try {
                    next = baseUrl.resolve(
                            part.substring(start + 1, end)
                    );
                } catch (IllegalArgumentException exception) {
                    throw new IOException(
                            "Canvas returned an invalid pagination link."
                    );
                }

                requireSameServer(next);
            }
        }

        return next;
    }

    private void requireSameServer(URI uri)
            throws IOException {

        int basePort = baseUrl.getPort() == -1
                ? 443
                : baseUrl.getPort();

        int targetPort = uri.getPort() == -1
                ? 443
                : uri.getPort();

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || !baseUrl.getHost().equalsIgnoreCase(uri.getHost())
                || basePort != targetPort
                || uri.getRawUserInfo() != null
                || uri.getRawFragment() != null
                || !uri.getPath().startsWith("/api/v1/")) {

            throw new IOException(
                    "Canvas pagination pointed outside this school's API."
            );
        }
    }

    private static URI normalizeUrl(String canvasUrl) {
        if (canvasUrl == null || canvasUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter your school's Canvas URL."
            );
        }

        try {
            URI entered = URI.create(canvasUrl.trim());
            int port = entered.getPort();

            if (!"https".equalsIgnoreCase(entered.getScheme())
                    || entered.getHost() == null
                    || entered.getRawUserInfo() != null
                    || port == 0
                    || port > 65535) {
                throw new IllegalArgumentException();
            }

            return new URI(
                    "https",
                    null,
                    entered.getHost(),
                    port,
                    "/",
                    null,
                    null
            );
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Enter a valid HTTPS Canvas URL with a port from 1 to 65535 if specified."
            );
        }
    }

    HttpRequest createGetRequest(String apiPath) {
        if (apiPath == null || !apiPath.startsWith("api/v1/")) {
            throw new IllegalArgumentException(
                    "Use a relative Canvas API path beginning with api/v1/."
            );
        }

        URI target;

        try {
            target = baseUrl.resolve(apiPath);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid Canvas API path."
            );
        }

        return buildRequest(target);
    }

    private HttpRequest buildRequest(URI target) {
        return HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .GET()
                .build();
    }
}

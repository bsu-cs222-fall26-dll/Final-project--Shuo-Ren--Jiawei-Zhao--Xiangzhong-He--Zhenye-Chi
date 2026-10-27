package compass.canvas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

final class CanvasAssignmentReader {
    private final CanvasApiService api;
    private final ZoneId zone;

    CanvasAssignmentReader(CanvasApiService api) {
        this(api, ZoneId.systemDefault());
    }

    CanvasAssignmentReader(CanvasApiService api, ZoneId zone) {
        this.api = Objects.requireNonNull(api);
        this.zone = Objects.requireNonNull(zone);
    }

    List<CanvasAssignment> fetch(List<CanvasCourse> selectedCourses)
            throws IOException, InterruptedException {

        if (selectedCourses == null) {
            throw new IllegalArgumentException(
                    "Select the courses to read."
            );
        }

        LinkedHashMap<Long, CanvasCourse> selected =
                new LinkedHashMap<>();

        // Validate the entire selection before sending requests.
        for (CanvasCourse course : selectedCourses) {
            if (course == null
                    || course.id() <= 0
                    || course.name() == null
                    || course.name().isBlank()) {

                throw new IllegalArgumentException(
                        "The course selection contains an invalid course."
                );
            }

            selected.putIfAbsent(course.id(), course);
        }

        LinkedHashMap<String, CanvasAssignment> result =
                new LinkedHashMap<>();

        for (CanvasCourse course : selected.values()) {
            String path = "api/v1/courses/"
                    + course.id()
                    + "/assignments?per_page=100";

            for (JsonElement element : api.getPages(path)) {
                if (!element.isJsonObject()) {
                    throw new IOException(
                            "Canvas returned an invalid assignment record."
                    );
                }

                JsonObject item = element.getAsJsonObject();
                long id = positiveId(item.get("id"));

                JsonElement courseId = item.get("course_id");

                if (courseId != null
                        && !courseId.isJsonNull()
                        && positiveId(courseId) != course.id()) {

                    throw new IOException(
                            "Canvas returned an assignment for a different course."
                    );
                }

                String name = assignmentName(item.get("name"));

                CanvasAssignment assignment = new CanvasAssignment(
                        id,
                        course.id(),
                        name,
                        course.name().trim(),
                        dueDate(item.get("due_at")),
                        points(item.get("points_possible"))
                );

                result.put(course.id() + ":" + id, assignment);
            }
        }

        return List.copyOf(result.values());
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
                    "Canvas returned a missing or invalid assignment/course ID."
            );
        }
    }

    private static String assignmentName(JsonElement value)
            throws IOException {

        if (value == null || value.isJsonNull()) {
            return "Untitled assignment";
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {

            throw new IOException(
                    "Canvas returned an invalid assignment name."
            );
        }

        String name = value.getAsString().trim();

        return name.isBlank() ? "Untitled assignment" : name;
    }

    private LocalDateTime dueDate(JsonElement value)
            throws IOException {

        if (value == null || value.isJsonNull()) {
            return null;
        }

        if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {

            throw new IOException(
                    "Canvas returned an invalid assignment due date."
            );
        }

        try {
            return OffsetDateTime.parse(value.getAsString())
                    .atZoneSameInstant(zone)
                    .toLocalDateTime();
        } catch (DateTimeParseException exception) {
            throw new IOException(
                    "Canvas returned an invalid assignment due date."
            );
        }
    }

    private static BigDecimal points(JsonElement value)
            throws IOException {

        if (value == null || value.isJsonNull()) {
            return BigDecimal.ZERO;
        }

        try {
            if (!value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isNumber()) {

                throw new IllegalArgumentException();
            }

            BigDecimal points = value.getAsBigDecimal();

            if (points.signum() < 0) {
                throw new IllegalArgumentException();
            }

            return points;
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Canvas returned invalid assignment points."
            );
        }
    }
}
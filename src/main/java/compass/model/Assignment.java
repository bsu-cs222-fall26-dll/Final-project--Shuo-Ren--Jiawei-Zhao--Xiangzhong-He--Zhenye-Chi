package compass.model;

import java.time.LocalDateTime;

public final class Assignment {

    private final long id;
    private final String name;
    private final String courseName;
    private final LocalDateTime dueDate;
    private final AssignmentSource source;
    private final Long canvasAssignmentId;

    public Assignment(
            long id,
            String name,
            String courseName,
            LocalDateTime dueDate
    ) {
        this(
                id,
                name,
                courseName,
                dueDate,
                AssignmentSource.MANUAL,
                null
        );
    }

    public Assignment(
            long id,
            String name,
            String courseName,
            LocalDateTime dueDate,
            AssignmentSource source,
            Long canvasAssignmentId
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Assignment name must not be blank."
            );
        }

        if (courseName == null || courseName.isBlank()) {
            throw new IllegalArgumentException(
                    "Course name must not be blank."
            );
        }
        this.id = id;
        this.name = name;
        this.courseName = courseName;
        this.dueDate = dueDate;
        this.source = source;
        this.canvasAssignmentId = canvasAssignmentId;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCourseName() {
        return courseName;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public AssignmentSource getSource() {
        return source;
    }

    public Long getCanvasAssignmentId() {
        return canvasAssignmentId;
    }
}
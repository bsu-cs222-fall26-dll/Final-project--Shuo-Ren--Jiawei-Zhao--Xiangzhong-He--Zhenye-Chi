package compass.model;

import java.time.LocalDateTime;

public final class Assignment {

    private final long id;
    private final String name;
    private final String courseName;
    private final LocalDateTime dueDate;

    public Assignment(
            long id,
            String name,
            String courseName,
            LocalDateTime dueDate
    ) {
        this.id = id;
        this.name = name;
        this.courseName = courseName;
        this.dueDate = dueDate;
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
}
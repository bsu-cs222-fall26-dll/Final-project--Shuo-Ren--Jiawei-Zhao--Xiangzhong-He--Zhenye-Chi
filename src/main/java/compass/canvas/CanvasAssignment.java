package compass.canvas;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** canvasAssignmentId is a remote Canvas ID, not Planner's local ID. */
public record CanvasAssignment(
        long canvasAssignmentId,
        long courseId,
        String name,
        String courseName,
        LocalDateTime dueDate,
        BigDecimal pointsPossible
) { }
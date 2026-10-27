package compass.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Planner {

    private final Map<Long, Assignment> assignments = new HashMap<>();
    private long nextAssignmentId = 1L;

    public Assignment createManualAssignment(
            String name,
            String courseName,
            LocalDateTime dueDate
    ) {
        Assignment assignment = new Assignment(
                nextAssignmentId,
                name,
                courseName,
                dueDate
        );

        assignments.put(assignment.getId(), assignment);
        nextAssignmentId++;
        return assignment;
    }

    public List<Assignment> getAllAssignments() {
        return assignments.values().stream()
                .sorted(Comparator.comparing(
                        Assignment::getDueDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).thenComparingLong(Assignment::getId))
                .toList();
    }
}
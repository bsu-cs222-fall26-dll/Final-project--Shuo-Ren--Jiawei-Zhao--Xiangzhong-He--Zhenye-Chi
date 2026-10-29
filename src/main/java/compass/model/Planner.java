package compass.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Planner {

    private final Map<Long, Assignment> assignments = new HashMap<>();
    private long nextAssignmentId = 1L;

    private final Map<Long, List<StudyStep>> stepsByAssignment =
            new HashMap<>();
    private long nextStudyStepId = 1L;

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

    /** Updates a Canvas assignment without replacing its local ID or study steps. */
    public Assignment importCanvasAssignment(
            String name, String courseName, LocalDateTime dueDate,
            long canvasAssignmentId
    ) {
        if (canvasAssignmentId <= 0) {
            throw new IllegalArgumentException("Canvas assignment ID must be positive.");
        }
        Assignment existing = assignments.values().stream()
                .filter(item -> item.getSource() == AssignmentSource.CANVAS
                        && Long.valueOf(canvasAssignmentId).equals(item.getCanvasAssignmentId()))
                .findFirst().orElse(null);
        long localId = existing == null ? nextAssignmentId : existing.getId();
        Assignment imported = new Assignment(localId, name, courseName, dueDate,
                AssignmentSource.CANVAS, canvasAssignmentId);
        assignments.put(localId, imported);
        if (existing == null) {
            nextAssignmentId++;
        }
        return imported;
    }

    public StudyStep addStudyStep(long assignmentId, String text) {
        if (!assignments.containsKey(assignmentId)) {
            throw new IllegalArgumentException(
                    "Assignment not found: " + assignmentId
            );
        }

        StudyStep step = new StudyStep(
                nextStudyStepId,
                assignmentId,
                text
        );

        stepsByAssignment
                .computeIfAbsent(assignmentId, ignored -> new ArrayList<>())
                .add(step);

        nextStudyStepId++;
        return step;
    }

    public List<StudyStep> getStepsForAssignment(long assignmentId) {
        return List.copyOf(
                stepsByAssignment.getOrDefault(assignmentId, List.of())
        );
    }

    public void deleteStudyStep(long assignmentId, long stepId) {
        List<StudyStep> steps = stepsByAssignment.get(assignmentId);

        if (steps == null
                || !steps.removeIf(step -> step.getId() == stepId)) {
            throw new IllegalArgumentException(
                    "Study step not found for assignment "
                            + assignmentId + ": " + stepId
            );
        }
    }

    public void setStudyStepCompleted(
            long assignmentId,
            long stepId,
            boolean completed
    ) {
        StudyStep step = getStepsForAssignment(assignmentId).stream()
                .filter(candidate -> candidate.getId() == stepId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Study step not found for assignment "
                                + assignmentId + ": " + stepId
                ));

        step.setCompleted(completed);
    }

    public void deleteAssignment(long assignmentId) {
        if (assignments.remove(assignmentId) == null) {
            throw new IllegalArgumentException(
                    "Assignment not found: " + assignmentId
            );
        }

        stepsByAssignment.remove(assignmentId);
    }
}

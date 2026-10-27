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
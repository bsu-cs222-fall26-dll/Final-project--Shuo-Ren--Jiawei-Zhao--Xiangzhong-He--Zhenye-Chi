
package compass.storage;

import compass.model.Assignment;
import compass.model.AssignmentSource;
import compass.model.StudyStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssignmentMergeService {

    public AppData mergeImportedAssignments(
            AppData savedData,
            List<Assignment> importedAssignments
    ) {
        return mergeImportedAssignments(
                savedData, importedAssignments, false
        );
    }

    public AppData mergeImportedAssignments(
            AppData savedData,
            List<Assignment> importedAssignments,
            boolean isReplaceMode
    ) {
        if (savedData == null || importedAssignments == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        for (Assignment imported : importedAssignments) {
            if (imported == null
                    || imported.getSource() != AssignmentSource.CANVAS
                    || imported.getCanvasAssignmentId() == null) {
                throw new IllegalArgumentException(
                        "Invalid Canvas assignment"
                );
            }
        }

        List<Assignment> assignments =
                new ArrayList<>(savedData.getAssignments());

        long nextId = 1;
        for (Assignment assignment : assignments) {
            nextId = Math.max(nextId, assignment.getId() + 1);
        }

        if (isReplaceMode) {
            Set<Long> selectedIds = new HashSet<>();

            for (Assignment imported : importedAssignments) {
                selectedIds.add(imported.getCanvasAssignmentId());
            }

            assignments.removeIf(assignment ->
                    assignment.getSource() == AssignmentSource.CANVAS
                            && !selectedIds.contains(
                            assignment.getCanvasAssignmentId()
                    )
            );
        }

        Map<Long, Integer> canvasIndex = new HashMap<>();

        for (int i = 0; i < assignments.size(); i++) {
            Assignment assignment = assignments.get(i);

            if (assignment.getSource() == AssignmentSource.CANVAS
                    && assignment.getCanvasAssignmentId() != null) {
                canvasIndex.put(assignment.getCanvasAssignmentId(), i);
            }
        }

        for (Assignment imported : importedAssignments) {
            Long canvasId = imported.getCanvasAssignmentId();
            Integer existingIndex = canvasIndex.get(canvasId);

            if (existingIndex != null) {
                Assignment existing = assignments.get(existingIndex);

                assignments.set(existingIndex, new Assignment(
                        existing.getId(),
                        imported.getName(),
                        imported.getCourseName(),
                        imported.getDueDate(),
                        AssignmentSource.CANVAS,
                        canvasId
                ));
            } else {
                Assignment newAssignment = new Assignment(
                        nextId++,
                        imported.getName(),
                        imported.getCourseName(),
                        imported.getDueDate(),
                        AssignmentSource.CANVAS,
                        canvasId
                );

                canvasIndex.put(canvasId, assignments.size());
                assignments.add(newAssignment);
            }
        }

        Set<Long> remainingIds = new HashSet<>();
        for (Assignment assignment : assignments) {
            remainingIds.add(assignment.getId());
        }

        Map<Long, List<StudyStep>> remainingSteps =
                new HashMap<>();

        savedData.getStepsByAssignment().forEach((id, steps) -> {
            if (remainingIds.contains(id)) {
                remainingSteps.put(id, new ArrayList<>(steps));
            }
        });

        return new AppData(assignments, remainingSteps);
    }
}

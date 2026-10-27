
package compass.storage;

import compass.model.Assignment;
import compass.model.AssignmentSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignmentMergeService {

    public AppData mergeImportedAssignments(
            AppData savedData,
            List<Assignment> importedAssignments
    ) {
        List<Assignment> assignments =
                new ArrayList<>(savedData.getAssignments());

        Map<Long, Integer> canvasIndex = new HashMap<>();

        long nextId = 1;

        for (int i = 0; i < assignments.size(); i++) {
            Assignment assignment = assignments.get(i);

            nextId = Math.max(nextId, assignment.getId() + 1);

            if (assignment.getSource() == AssignmentSource.CANVAS
                    && assignment.getCanvasAssignmentId() != null) {
                canvasIndex.put(assignment.getCanvasAssignmentId(), i);
            }
        }

        for (Assignment imported : importedAssignments) {
            if (imported == null
                    || imported.getSource() != AssignmentSource.CANVAS
                    || imported.getCanvasAssignmentId() == null) {
                throw new IllegalArgumentException(
                        "Invalid Canvas assignment"
                );
            }

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

        return new AppData(
                assignments,
                savedData.getStepsByAssignment()
        );
    }
}

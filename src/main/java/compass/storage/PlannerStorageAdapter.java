
package compass.storage;

import compass.model.Assignment;
import compass.model.Planner;
import compass.model.StudyStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlannerStorageAdapter {

    public AppData capture(Planner planner) {
        if (planner == null) {
            throw new IllegalArgumentException(
                    "Planner cannot be null"
            );
        }

        List<Assignment> assignments =
                new ArrayList<>(planner.getAllAssignments());

        Map<Long, List<StudyStep>> stepsByAssignment =
                new HashMap<>();

        for (Assignment assignment : assignments) {
            List<StudyStep> copiedSteps = new ArrayList<>();

            for (StudyStep step : planner.getStepsForAssignment(
                    assignment.getId())) {

                StudyStep copy = new StudyStep(
                        step.getId(),
                        step.getAssignmentId(),
                        step.getText()
                );

                copy.setCompleted(step.isCompleted());
                copiedSteps.add(copy);
            }

            stepsByAssignment.put(
                    assignment.getId(),
                    copiedSteps
            );
        }

        return new AppData(assignments, stepsByAssignment);
    }
}

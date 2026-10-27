
package compass.storage;

import compass.model.Assignment;
import compass.model.StudyStep;

import java.util.List;
import java.util.Map;

public class AppData {

    private final List<Assignment> assignments;
    private final Map<Long, List<StudyStep>> stepsByAssignment;

    public AppData(
            List<Assignment> assignments,
            Map<Long, List<StudyStep>> stepsByAssignment
    ) {
        this.assignments = assignments;
        this.stepsByAssignment = stepsByAssignment;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public Map<Long, List<StudyStep>> getStepsByAssignment() {
        return stepsByAssignment;
    }
}

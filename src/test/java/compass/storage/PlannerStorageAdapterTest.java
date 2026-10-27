
package compass.storage;

import compass.model.Assignment;
import compass.model.Planner;
import compass.model.StudyStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class PlannerStorageAdapterTest {

    @Test
    void savesPlannerAssignmentsAndSteps(@TempDir Path tempDir)
            throws IOException {

        Planner planner = new Planner();

        Assignment assignment = planner.createManualAssignment(
                "Java Homework",
                "CS222",
                LocalDateTime.of(2026, 11, 10, 23, 59)
        );

        StudyStep step = planner.addStudyStep(
                assignment.getId(),
                "Finish the code"
        );

        planner.setStudyStepCompleted(
                assignment.getId(),
                step.getId(),
                true
        );

        PlannerStorageAdapter adapter =
                new PlannerStorageAdapter();

        AppData data = adapter.capture(planner);

        LocalStorageService storage = new LocalStorageService(
                tempDir.resolve("planner.json")
        );

        storage.saveAllData(data);
        AppData loaded = storage.loadAllData();

        assertEquals(1, loaded.getAssignments().size());
        assertEquals("Java Homework",
                loaded.getAssignments().get(0).getName());

        assertEquals(1,
                loaded.getStepsByAssignment()
                        .get(assignment.getId()).size());

        assertTrue(
                loaded.getStepsByAssignment()
                        .get(assignment.getId()).get(0)
                        .isCompleted()
        );
    }

    @Test
    void capturedDataDoesNotChangeAfterPlannerUpdates() {
        Planner planner = new Planner();

        Assignment assignment = planner.createManualAssignment(
                "Java Homework",
                "CS222",
                LocalDateTime.of(2026, 11, 10, 23, 59)
        );

        StudyStep step = planner.addStudyStep(
                assignment.getId(),
                "Finish the code"
        );

        PlannerStorageAdapter adapter =
                new PlannerStorageAdapter();

        AppData snapshot = adapter.capture(planner);

        planner.setStudyStepCompleted(
                assignment.getId(),
                step.getId(),
                true
        );

        assertFalse(
                snapshot.getStepsByAssignment()
                        .get(assignment.getId())
                        .get(0)
                        .isCompleted()
        );

        assertTrue(
                planner.getStepsForAssignment(assignment.getId())
                        .get(0)
                        .isCompleted()
        );
    }

}

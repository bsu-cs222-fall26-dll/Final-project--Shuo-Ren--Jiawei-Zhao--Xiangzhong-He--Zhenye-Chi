package compass.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlannerAssignmentDeletionTest {

    private final Planner planner = new Planner();
    private final Assignment first =
            planner.createManualAssignment("Lab 1", "CS222", null);
    private final Assignment second =
            planner.createManualAssignment("Lab 2", "CS222", null);

    @Test
    void deletesAssignmentAndItsStepsWhilePreservingOthers() {
        StudyStep removed = planner.addStudyStep(first.getId(), "Remove");
        StudyStep kept = planner.addStudyStep(second.getId(), "Keep");
        planner.setStudyStepCompleted(second.getId(), kept.getId(), true);

        planner.deleteAssignment(first.getId());

        assertEquals(List.of(second), planner.getAllAssignments());
        assertTrue(planner.getStepsForAssignment(first.getId()).isEmpty());
        assertEquals(
                List.of(kept),
                planner.getStepsForAssignment(second.getId())
        );
        assertTrue(kept.isCompleted());

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.addStudyStep(first.getId(), "New step")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> planner.setStudyStepCompleted(
                        first.getId(), removed.getId(), true
                )
        );
    }

    @Test
    void deletesAssignmentWithoutSteps() {
        planner.deleteAssignment(first.getId());

        assertEquals(List.of(second), planner.getAllAssignments());
        assertTrue(planner.getStepsForAssignment(first.getId()).isEmpty());
    }

    @Test
    void rejectsUnknownAssignmentWithoutChangingData() {
        StudyStep firstStep = planner.addStudyStep(first.getId(), "First");
        StudyStep secondStep = planner.addStudyStep(second.getId(), "Second");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> planner.deleteAssignment(999L)
        );

        assertEquals("Assignment not found: 999", error.getMessage());
        assertEquals(List.of(first, second), planner.getAllAssignments());
        assertEquals(
                List.of(firstStep),
                planner.getStepsForAssignment(first.getId())
        );
        assertEquals(
                List.of(secondStep),
                planner.getStepsForAssignment(second.getId())
        );
    }
}
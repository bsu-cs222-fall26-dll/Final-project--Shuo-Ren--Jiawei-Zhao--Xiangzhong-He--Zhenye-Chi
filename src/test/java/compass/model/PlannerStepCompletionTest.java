package compass.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlannerStepCompletionTest {

    private final Planner planner = new Planner();
    private final Assignment first =
            planner.createManualAssignment("Lab 1", "CS222", null);
    private final Assignment second =
            planner.createManualAssignment("Lab 2", "CS222", null);

    @Test
    void updatesOnlySelectedStepAndAllowsUnchecking() {
        StudyStep selected = planner.addStudyStep(first.getId(), "Selected");
        StudyStep unchanged = planner.addStudyStep(first.getId(), "Unchanged");
        StudyStep other = planner.addStudyStep(second.getId(), "Other");

        planner.setStudyStepCompleted(first.getId(), selected.getId(), true);

        assertTrue(selected.isCompleted());
        assertFalse(unchanged.isCompleted());
        assertFalse(other.isCompleted());

        planner.setStudyStepCompleted(first.getId(), selected.getId(), false);

        assertFalse(selected.isCompleted());
        assertFalse(unchanged.isCompleted());
        assertFalse(other.isCompleted());
    }

    @Test
    void rejectsMissingStepsWithoutChangingExistingState() {
        StudyStep existing = planner.addStudyStep(first.getId(), "Keep");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> planner.setStudyStepCompleted(first.getId(), 999L, true)
        );

        assertEquals(
                "Study step not found for assignment "
                        + first.getId() + ": 999",
                error.getMessage()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.setStudyStepCompleted(
                        999L, existing.getId(), true
                )
        );

        assertFalse(existing.isCompleted());
    }

    @Test
    void rejectsStepsBelongingToAnotherAssignment() {
        StudyStep own = planner.addStudyStep(first.getId(), "Own");
        StudyStep other = planner.addStudyStep(second.getId(), "Other");

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.setStudyStepCompleted(
                        first.getId(), other.getId(), true
                )
        );

        assertFalse(own.isCompleted());
        assertFalse(other.isCompleted());
    }
}
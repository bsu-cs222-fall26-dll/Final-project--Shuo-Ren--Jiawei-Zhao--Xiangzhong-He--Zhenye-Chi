package compass.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlannerStudyStepTest {

    private final Planner planner = new Planner();
    private final Assignment first =
            planner.createManualAssignment("Lab 1", "CS222", null);
    private final Assignment second =
            planner.createManualAssignment("Lab 2", "CS222", null);

    @Test
    void keepsStepsSeparatedByAssignment() {
        assertTrue(planner.getStepsForAssignment(first.getId()).isEmpty());

        StudyStep read = planner.addStudyStep(first.getId(), "Read");
        StudyStep write = planner.addStudyStep(first.getId(), "Write");
        StudyStep review = planner.addStudyStep(second.getId(), "Review");

        assertEquals(first.getId(), read.getAssignmentId());
        assertEquals("Read", read.getText());
        assertFalse(read.isCompleted());
        assertNotEquals(read.getId(), write.getId());
        assertNotEquals(read.getId(), review.getId());

        assertEquals(
                List.of(read, write),
                planner.getStepsForAssignment(first.getId())
        );
        assertEquals(
                List.of(review),
                planner.getStepsForAssignment(second.getId())
        );
    }

    @Test
    void rejectsInvalidStepCreationWithoutChangingExistingSteps() {
        StudyStep kept = planner.addStudyStep(first.getId(), "Keep");

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.addStudyStep(999L, "Read")
        );

        for (String text : new String[]{null, "", " ", "\t"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> planner.addStudyStep(first.getId(), text)
            );
        }

        assertEquals(
                List.of(kept),
                planner.getStepsForAssignment(first.getId())
        );
    }

    @Test
    void deletesOnlyTheSelectedStep() {
        StudyStep removed = planner.addStudyStep(first.getId(), "Remove");
        StudyStep kept = planner.addStudyStep(first.getId(), "Keep");
        StudyStep other = planner.addStudyStep(second.getId(), "Other");

        planner.deleteStudyStep(first.getId(), removed.getId());

        assertEquals(
                List.of(kept),
                planner.getStepsForAssignment(first.getId())
        );
        assertEquals(
                List.of(other),
                planner.getStepsForAssignment(second.getId())
        );
    }

    @Test
    void rejectsMissingOrUnrelatedStepsWithoutDeletingData() {
        StudyStep own = planner.addStudyStep(first.getId(), "Own");
        StudyStep other = planner.addStudyStep(second.getId(), "Other");

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.deleteStudyStep(first.getId(), 999L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> planner.deleteStudyStep(first.getId(), other.getId())
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> planner.deleteStudyStep(999L, own.getId())
        );

        assertEquals(
                List.of(own),
                planner.getStepsForAssignment(first.getId())
        );
        assertEquals(
                List.of(other),
                planner.getStepsForAssignment(second.getId())
        );
    }
}
package compass.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StudyStepTest {

    @Test
    void storesDetailsAndStartsIncomplete() {
        StudyStep step = new StudyStep(201L, 101L, "Read chapter 1");

        assertEquals(201L, step.getId());
        assertEquals(101L, step.getAssignmentId());
        assertEquals("Read chapter 1", step.getText());
        assertFalse(step.isCompleted());
    }

    @Test
    void updatesCompletionState() {
        StudyStep step = new StudyStep(201L, 101L, "Read chapter 1");

        step.setCompleted(true);
        assertTrue(step.isCompleted());

        step.setCompleted(false);
        assertFalse(step.isCompleted());
    }

    @Test
    void rejectsBlankText() {
        for (String text : new String[]{null, "", " ", "\t"}) {
            IllegalArgumentException error = assertThrows(
                    IllegalArgumentException.class,
                    () -> new StudyStep(201L, 101L, text)
            );

            assertEquals(
                    "Study step text must not be blank.",
                    error.getMessage()
            );
        }
    }
}
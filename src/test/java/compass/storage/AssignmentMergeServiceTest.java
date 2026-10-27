
package compass.storage;

import compass.model.Assignment;
import compass.model.AssignmentSource;
import compass.model.StudyStep;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AssignmentMergeServiceTest {

    @Test
    void mergesCanvasAssignmentsWithoutLosingSteps() {
        Assignment manual = new Assignment(
                1L, "Homework", "CS222", null
        );

        Assignment existing = new Assignment(
                2L, "Old Project", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        StudyStep step = new StudyStep(
                3L, 2L, "Finish the code"
        );
        step.setCompleted(true);

        AppData saved = new AppData(
                List.of(manual, existing),
                Map.of(2L, List.of(step))
        );

        Assignment updated = new Assignment(
                99L, "Updated Project", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        Assignment newAssignment = new Assignment(
                98L, "New Assignment", "CS222", null,
                AssignmentSource.CANVAS, 200L
        );

        AssignmentMergeService service =
                new AssignmentMergeService();

        AppData result = service.mergeImportedAssignments(
                saved,
                List.of(updated, newAssignment)
        );

        assertEquals(3, result.getAssignments().size());

        assertEquals("Homework",
                result.getAssignments().get(0).getName());

        assertEquals(2L,
                result.getAssignments().get(1).getId());

        assertEquals("Updated Project",
                result.getAssignments().get(1).getName());

        assertEquals(3L,
                result.getAssignments().get(2).getId());

        assertEquals(200L,
                result.getAssignments().get(2).getCanvasAssignmentId());

        StudyStep restoredStep =
                result.getStepsByAssignment().get(2L).get(0);

        assertEquals("Finish the code", restoredStep.getText());
        assertTrue(restoredStep.isCompleted());
    }

    @Test
    void doesNotDuplicateCanvasAssignments() {
        AppData saved = new AppData(List.of(), Map.of());

        Assignment first = new Assignment(
                10L, "Old Homework", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        Assignment updated = new Assignment(
                20L, "Updated Homework", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        AssignmentMergeService service =
                new AssignmentMergeService();

        AppData result = service.mergeImportedAssignments(
                saved,
                List.of(first, updated)
        );

        assertEquals(1, result.getAssignments().size());
        assertEquals(1L, result.getAssignments().get(0).getId());
        assertEquals("Updated Homework",
                result.getAssignments().get(0).getName());
    }

    @Test
    void rejectsCanvasAssignmentWithoutId() {
        AppData saved = new AppData(List.of(), Map.of());

        Assignment invalid = new Assignment(
                1L,
                "Java Homework",
                "CS222",
                null,
                AssignmentSource.CANVAS,
                null
        );

        AssignmentMergeService service =
                new AssignmentMergeService();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.mergeImportedAssignments(
                        saved,
                        List.of(invalid)
                )
        );

        assertTrue(saved.getAssignments().isEmpty());
    }

    @Test
    void replacesOldCanvasAssignmentsButKeepsManualTasks() {
        Assignment manual = new Assignment(
                1L, "My Homework", "CS222", null
        );

        Assignment oldCanvas = new Assignment(
                2L, "Old Canvas Task", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        StudyStep step = new StudyStep(
                3L, 1L, "Finish homework"
        );
        step.setCompleted(true);

        AppData saved = new AppData(
                List.of(manual, oldCanvas),
                Map.of(1L, List.of(step))
        );

        Assignment newCanvas = new Assignment(
                10L, "New Canvas Task", "CS222", null,
                AssignmentSource.CANVAS, 200L
        );

        AssignmentMergeService service =
                new AssignmentMergeService();

        AppData result = service.mergeImportedAssignments(
                saved, List.of(newCanvas), true
        );

        assertEquals(2, result.getAssignments().size());
        assertTrue(result.getAssignments().stream()
                .anyMatch(a -> a.getSource() == AssignmentSource.MANUAL));
        assertTrue(result.getAssignments().stream()
                .noneMatch(a -> Long.valueOf(100L).equals(a.getCanvasAssignmentId())));
        assertTrue(result.getAssignments().stream()
                .anyMatch(a -> Long.valueOf(200L).equals(a.getCanvasAssignmentId())));
        assertTrue(result.getStepsByAssignment().get(1L).get(0).isCompleted());
    }

    @Test
    void keepsSelectedCanvasStepsWhenReplacing() {
        Assignment kept = new Assignment(
                1L, "Old Project", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        Assignment removed = new Assignment(
                2L, "Old Homework", "CS222", null,
                AssignmentSource.CANVAS, 200L
        );

        StudyStep step = new StudyStep(
                3L, 1L, "Finish the project"
        );
        step.setCompleted(true);

        AppData saved = new AppData(
                List.of(kept, removed),
                Map.of(1L, List.of(step))
        );

        Assignment updated = new Assignment(
                10L, "Updated Project", "CS222", null,
                AssignmentSource.CANVAS, 100L
        );

        AssignmentMergeService service =
                new AssignmentMergeService();

        AppData result = service.mergeImportedAssignments(
                saved, List.of(updated), true
        );

        assertEquals(1, result.getAssignments().size());
        assertEquals(1L, result.getAssignments().get(0).getId());
        assertEquals("Updated Project",
                result.getAssignments().get(0).getName());

        assertEquals(1, result.getStepsByAssignment().get(1L).size());
        assertTrue(result.getStepsByAssignment()
                .get(1L).get(0).isCompleted());

        assertFalse(result.getStepsByAssignment().containsKey(2L));
    }

}

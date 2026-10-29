package compass.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PlannerCanvasImportTest {
    @Test
    void canvasAndManualAssignmentsUseDifferentLocalIdsEvenWhenRemoteIdMatches() {
        Planner planner = new Planner();
        Assignment manual = planner.createManualAssignment("Manual", "CS222", null);
        Assignment imported = planner.importCanvasAssignment("Project", "CS222", null, manual.getId());

        assertNotEquals(manual.getId(), imported.getId());
        assertEquals(manual.getId(), imported.getCanvasAssignmentId());
        assertEquals(AssignmentSource.CANVAS, imported.getSource());
        assertNull(imported.getDueDate());
        assertEquals(2, planner.getAllAssignments().size());
    }

    @Test
    void reimportUpdatesMetadataAndPreservesLocalIdAndCompletedSteps() {
        Planner planner = new Planner();
        Assignment original = planner.importCanvasAssignment("Draft", "Old course", null, 901);
        StudyStep step = planner.addStudyStep(original.getId(), "Read the instructions");
        planner.setStudyStepCompleted(original.getId(), step.getId(), true);
        LocalDateTime deadline = LocalDateTime.of(2026, 11, 5, 17, 0);

        Assignment updated = planner.importCanvasAssignment("Final", "CS222", deadline, 901);

        assertEquals(original.getId(), updated.getId());
        assertEquals(1, planner.getAllAssignments().size());
        assertEquals("Final", updated.getName());
        assertEquals("CS222", updated.getCourseName());
        assertEquals(deadline, updated.getDueDate());
        assertEquals(step.getId(), planner.getStepsForAssignment(updated.getId()).get(0).getId());
        assertTrue(planner.getStepsForAssignment(updated.getId()).get(0).isCompleted());
    }

    @Test
    void reimportDoesNotChangeAnotherAssignmentOrConsumeAnExtraLocalId() {
        Planner planner = new Planner();
        Assignment first = planner.importCanvasAssignment("First", "CS222", null, 901);
        Assignment second = planner.importCanvasAssignment("Second", "CS222", null, 902);
        StudyStep otherStep = planner.addStudyStep(second.getId(), "Keep this step");

        planner.importCanvasAssignment("Updated first", "CS222", null, 901);
        Assignment manual = planner.createManualAssignment("Manual", "CS222", null);

        assertEquals(first.getId() + 2, manual.getId());
        assertEquals("Second", planner.getAllAssignments().get(1).getName());
        assertEquals(otherStep, planner.getStepsForAssignment(second.getId()).get(0));
    }

    @Test
    void rejectedImportLeavesAssignmentsAndIdCounterUnchanged() {
        Planner planner = new Planner();
        assertThrows(IllegalArgumentException.class,
                () -> planner.importCanvasAssignment("Invalid", "CS222", null, 0));
        assertThrows(IllegalArgumentException.class,
                () -> planner.importCanvasAssignment(" ", "CS222", null, 901));
        assertTrue(planner.getAllAssignments().isEmpty());
        assertEquals(1, planner.createManualAssignment("Valid", "CS222", null).getId());
    }
}

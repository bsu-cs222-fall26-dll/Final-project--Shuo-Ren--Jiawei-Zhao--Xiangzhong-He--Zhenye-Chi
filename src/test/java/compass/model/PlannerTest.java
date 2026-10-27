package compass.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    @Test
    void createsManualAssignmentAndAddsItToList() {
        Planner planner = new Planner();
        assertTrue(planner.getAllAssignments().isEmpty());

        LocalDateTime dueDate =
                LocalDateTime.of(2026, 11, 10, 23, 59);

        Assignment assignment =
                planner.createManualAssignment("Lab 1", "CS222", dueDate);

        assertEquals("Lab 1", assignment.getName());
        assertEquals("CS222", assignment.getCourseName());
        assertEquals(dueDate, assignment.getDueDate());
        assertEquals(AssignmentSource.MANUAL, assignment.getSource());
        assertNull(assignment.getCanvasAssignmentId());
        assertEquals(List.of(assignment), planner.getAllAssignments());
    }

    @Test
    void sortsByDueDateWithUndatedAssignmentsLast() {
        Planner planner = new Planner();
        LocalDateTime earlyDate =
                LocalDateTime.of(2026, 11, 10, 23, 59);
        LocalDateTime lateDate = earlyDate.plusDays(2);

        Assignment late =
                planner.createManualAssignment("Later", "CS222", lateDate);
        Assignment undated =
                planner.createManualAssignment("Reading", "CS222", null);
        Assignment early =
                planner.createManualAssignment("Earlier", "CS222", earlyDate);
        Assignment sameDate =
                planner.createManualAssignment("Also early", "CS222", earlyDate);

        assertNotEquals(early.getId(), sameDate.getId());
        assertEquals(
                List.of(early, sameDate, late, undated),
                planner.getAllAssignments()
        );
    }

    @Test
    void invalidAssignmentsAreNotAdded() {
        Planner planner = new Planner();

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.createManualAssignment("", "CS222", null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> planner.createManualAssignment("Lab 1", "", null)
        );

        assertTrue(planner.getAllAssignments().isEmpty());
    }
}
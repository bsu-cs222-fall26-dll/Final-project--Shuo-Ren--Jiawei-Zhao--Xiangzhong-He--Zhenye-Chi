package compass.model;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignmentTest {

    @Test
    void storesAssignmentDetails() {
        LocalDateTime dueDate =
                LocalDateTime.of(2026, 11, 10, 23, 59);

        Assignment assignment = new Assignment(
                101L,
                "Lab 1",
                "CS222",
                dueDate
        );

        assertEquals(101L, assignment.getId());
        assertEquals("Lab 1", assignment.getName());
        assertEquals("CS222", assignment.getCourseName());
        assertEquals(dueDate, assignment.getDueDate());
    }
    @Test
    void allowsAssignmentWithoutDueDate() {
        Assignment assignment = new Assignment(
                102L,
                "Reading",
                "CS222",
                null
        );

        assertNull(assignment.getDueDate());
    }
    @Test
    void manualAssignmentHasManualSourceAndNoCanvasId() {
        Assignment assignment = new Assignment(
                103L,
                "Practice",
                "CS222",
                null
        );

        assertEquals(AssignmentSource.MANUAL, assignment.getSource());
        assertNull(assignment.getCanvasAssignmentId());
    }

    @Test
    void canvasAssignmentPreservesSourceAndCanvasId() {
        Assignment assignment = new Assignment(
                104L,
                "Canvas Reading",
                "CS222",
                null,
                AssignmentSource.CANVAS,
                9001L
        );

        assertEquals(104L, assignment.getId());
        assertEquals(AssignmentSource.CANVAS, assignment.getSource());
        assertEquals(
                Long.valueOf(9001L),
                assignment.getCanvasAssignmentId()
        );
        assertNull(assignment.getDueDate());
    }
}
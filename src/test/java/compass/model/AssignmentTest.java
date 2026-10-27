package compass.model;

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
}
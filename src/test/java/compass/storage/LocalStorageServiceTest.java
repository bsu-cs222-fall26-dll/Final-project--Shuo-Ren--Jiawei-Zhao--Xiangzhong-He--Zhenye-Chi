
package compass.storage;

import compass.model.Assignment;
import compass.model.AssignmentSource;
import compass.model.StudyStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsText() throws IOException {
        Path file = tempDir.resolve("data").resolve("planner.txt");
        LocalStorageService storage = new LocalStorageService(file);

        storage.saveText("Course Compass");

        assertEquals("Course Compass", storage.loadText());
    }

    @Test
    void savesAndLoadsAppData() throws IOException {
        Path file = tempDir.resolve("planner.json");
        LocalStorageService storage = new LocalStorageService(file);

        LocalDateTime dueDate =
                LocalDateTime.of(2026, 11, 10, 23, 59);

        Assignment assignment = new Assignment(
                5L,
                "Java Project",
                "CS222",
                dueDate,
                AssignmentSource.CANVAS,
                12345L
        );

        StudyStep step = new StudyStep(
                8L,
                5L,
                "Finish the code"
        );
        step.setCompleted(true);

        AppData original = new AppData(
                List.of(assignment),
                Map.of(5L, List.of(step))
        );

        storage.saveAllData(original);
        AppData restored = storage.loadAllData();

        assertEquals(1, restored.getAssignments().size());

        Assignment savedAssignment =
                restored.getAssignments().get(0);

        assertEquals(5L, savedAssignment.getId());
        assertEquals("Java Project", savedAssignment.getName());
        assertEquals("CS222", savedAssignment.getCourseName());
        assertEquals(dueDate, savedAssignment.getDueDate());
        assertEquals(AssignmentSource.CANVAS,
                savedAssignment.getSource());
        assertEquals(12345L,
                savedAssignment.getCanvasAssignmentId());

        StudyStep savedStep =
                restored.getStepsByAssignment().get(5L).get(0);

        assertEquals(8L, savedStep.getId());
        assertEquals(5L, savedStep.getAssignmentId());
        assertEquals("Finish the code", savedStep.getText());
        assertTrue(savedStep.isCompleted());
    }

    @Test
    void rejectsCorruptedFileWithoutDeletingIt() throws IOException {
        Path file = tempDir.resolve("corrupted.json");
        String invalidJson = "{ invalid json";

        java.nio.file.Files.writeString(file, invalidJson);

        LocalStorageService storage = new LocalStorageService(file);

        assertThrows(IOException.class, storage::loadAllData);

        assertEquals(
                invalidJson,
                java.nio.file.Files.readString(file)
        );
    }

    @Test
    void rejectsMissingSaveFile() {
        Path file = tempDir.resolve("missing.json");
        LocalStorageService storage = new LocalStorageService(file);

        assertThrows(IOException.class, storage::loadAllData);
    }

    @Test
    void rejectsEmptySaveFile() throws IOException {
        Path file = tempDir.resolve("empty.json");
        java.nio.file.Files.writeString(file, "");

        LocalStorageService storage = new LocalStorageService(file);

        assertThrows(IOException.class, storage::loadAllData);
    }

}


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

    @Test
    void createsBackupBeforeReplacingSavedData() throws IOException {
        Path file = tempDir.resolve("planner.json");
        LocalStorageService storage = new LocalStorageService(file);

        AppData first = new AppData(List.of(), Map.of());
        storage.saveAllData(first);

        String originalJson = java.nio.file.Files.readString(file);

        AppData second = new AppData(
                List.of(new Assignment(
                        1L,
                        "CS222 Project",
                        "CS222",
                        null
                )),
                Map.of()
        );

        storage.saveAllData(second);

        Path backup = file.resolveSibling("planner.json.bak");

        assertTrue(java.nio.file.Files.exists(backup));
        assertEquals(
                originalJson,
                java.nio.file.Files.readString(backup)
        );
    }

    @Test
    void keepsOldDataWhenBackupFails() throws IOException {
        Path file = tempDir.resolve("planner.json");
        LocalStorageService storage = new LocalStorageService(file);

        AppData original = new AppData(List.of(), Map.of());
        storage.saveAllData(original);

        String oldJson = java.nio.file.Files.readString(file);

        Path backup = tempDir.resolve("planner.json.bak");
        java.nio.file.Files.createDirectories(backup);
        java.nio.file.Files.writeString(
                backup.resolve("keep.txt"), "backup"
        );

        AppData updated = new AppData(
                List.of(new Assignment(
                        2L, "New Project", "CS222", null
                )),
                Map.of()
        );

        assertThrows(
                IOException.class,
                () -> storage.saveAllData(updated)
        );

        assertEquals(
                oldJson,
                java.nio.file.Files.readString(file)
        );
    }

    @Test
    void restoresDataFromBackup() throws IOException {
        Path file = tempDir.resolve("planner.json");
        LocalStorageService storage = new LocalStorageService(file);

        AppData original = new AppData(
                List.of(new Assignment(
                        5L, "Java Project", "CS222", null
                )),
                Map.of()
        );

        storage.saveAllData(original);
        storage.saveAllData(new AppData(List.of(), Map.of()));

        java.nio.file.Files.writeString(file, "{ broken json");

        storage.restoreFromBackup();

        AppData restored = storage.loadAllData();

        assertEquals(1, restored.getAssignments().size());
        assertEquals(5L, restored.getAssignments().get(0).getId());
        assertEquals("Java Project",
                restored.getAssignments().get(0).getName());
    }

    @Test
    void keepsCurrentDataWhenBackupIsCorrupted() throws IOException {
        Path file = tempDir.resolve("planner.json");
        LocalStorageService storage = new LocalStorageService(file);

        AppData first = new AppData(List.of(), Map.of());
        storage.saveAllData(first);

        AppData current = new AppData(
                List.of(new Assignment(
                        1L, "CS222 Project", "CS222", null
                )),
                Map.of()
        );
        storage.saveAllData(current);

        String currentJson = java.nio.file.Files.readString(file);

        Path backup = tempDir.resolve("planner.json.bak");
        java.nio.file.Files.writeString(backup, "{ broken json");

        assertThrows(IOException.class, storage::restoreFromBackup);

        assertEquals(
                currentJson,
                java.nio.file.Files.readString(file)
        );
    }

    @Test
    void restoresDeletedAssignmentFromBackup(
            @TempDir Path tempDir
    ) throws IOException {

        Path file = tempDir.resolve("data.json");
        LocalStorageService storage =
                new LocalStorageService(file);

        Assignment first = new Assignment(
                1L, "Java Homework", "CS222", null
        );

        Assignment second = new Assignment(
                2L, "Final Project", "CS222", null
        );

        storage.saveAllData(new AppData(
                List.of(first, second),
                Map.of()
        ));

        storage.saveAllData(new AppData(
                List.of(first),
                Map.of()
        ));

        assertEquals(1,
                storage.loadAllData().getAssignments().size());

        storage.restoreFromBackup();

        AppData restored = storage.loadAllData();

        assertEquals(2, restored.getAssignments().size());

        assertTrue(restored.getAssignments().stream()
                .anyMatch(a -> a.getId() == 2L
                        && a.getName().equals("Final Project")));
    }

}

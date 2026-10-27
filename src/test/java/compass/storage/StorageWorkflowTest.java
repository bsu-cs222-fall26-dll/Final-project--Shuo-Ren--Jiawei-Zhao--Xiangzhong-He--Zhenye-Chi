
package compass.storage;

import compass.model.Assignment;
import compass.model.AssignmentSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StorageWorkflowTest {

    @Test
    void mergesSavesAndRestoresBackup(@TempDir Path tempDir)
            throws IOException {

        LocalStorageService storage = new LocalStorageService(
                tempDir.resolve("data.json")
        );

        Assignment manual = new Assignment(
                1L, "My Homework", "CS222", null
        );

        AppData original = new AppData(
                List.of(manual),
                Map.of()
        );

        storage.saveAllData(original);

        Assignment canvas = new Assignment(
                99L, "Canvas Project", "CS222", null,
                AssignmentSource.CANVAS, 500L
        );

        StorageWorkflow workflow = new StorageWorkflow(storage);

        AppData merged = workflow.mergeAndSave(
                original,
                List.of(canvas),
                false
        );

        assertEquals(2, merged.getAssignments().size());

        AppData loaded = storage.loadAllData();

        assertEquals(2, loaded.getAssignments().size());

        assertTrue(loaded.getAssignments().stream()
                .anyMatch(a -> Long.valueOf(500L)
                        .equals(a.getCanvasAssignmentId())));

        storage.restoreFromBackup();

        AppData restored = storage.loadAllData();

        assertEquals(1, restored.getAssignments().size());

        assertEquals("My Homework",
                restored.getAssignments().get(0).getName());
    }

    @Test
    void rejectsInvalidImportWithoutChangingSavedData(
            @TempDir Path tempDir
    ) throws IOException {

        LocalStorageService storage = new LocalStorageService(
                tempDir.resolve("data.json")
        );

        Assignment manual = new Assignment(
                1L, "My Homework", "CS222", null
        );

        AppData original = new AppData(
                List.of(manual),
                Map.of()
        );

        storage.saveAllData(original);

        Assignment invalid = new Assignment(
                2L, "Invalid Canvas Task", "CS222", null,
                AssignmentSource.CANVAS, null
        );

        StorageWorkflow workflow = new StorageWorkflow(storage);

        assertThrows(IllegalArgumentException.class,
                () -> workflow.mergeAndSave(
                        original,
                        List.of(invalid),
                        false
                )
        );

        AppData loaded = storage.loadAllData();

        assertEquals(1, loaded.getAssignments().size());
        assertEquals("My Homework",
                loaded.getAssignments().get(0).getName());
    }
}

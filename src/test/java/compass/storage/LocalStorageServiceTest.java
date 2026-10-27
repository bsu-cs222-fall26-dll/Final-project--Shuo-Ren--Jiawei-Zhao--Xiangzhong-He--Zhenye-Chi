
package compass.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}


package compass.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalStorageService {

    private final Path filePath;

    public LocalStorageService(Path filePath) {
        this.filePath = filePath;
    }

    public void saveText(String content) throws IOException {
        Path parent = filePath.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Files.writeString(filePath, content);
    }

    public String loadText() throws IOException {
        return Files.readString(filePath);
    }
}

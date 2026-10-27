
package compass.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

public class LocalStorageService {

    private final Path filePath;
    private final Gson gson;

    public LocalStorageService(Path filePath) {
        this.filePath = filePath;

        this.gson = new GsonBuilder()
                .registerTypeAdapter(
                        LocalDateTime.class,
                        (com.google.gson.JsonSerializer<LocalDateTime>)
                                (date, type, context) ->
                                        context.serialize(date.toString())
                )
                .registerTypeAdapter(
                        LocalDateTime.class,
                        (com.google.gson.JsonDeserializer<LocalDateTime>)
                                (json, type, context) ->
                                        LocalDateTime.parse(json.getAsString())
                )
                .setPrettyPrinting()
                .create();
    }

    public void saveText(String content) throws IOException {
        Path parent = filePath.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }

    public String loadText() throws IOException {
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    public void saveAllData(AppData data) throws IOException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        String json = gson.toJson(data);
        Path target = filePath.toAbsolutePath();
        Path parent = target.getParent();

        Files.createDirectories(parent);

        Path tempFile = Files.createTempFile(
                parent, "coursecompass-", ".tmp"
        );

        try {
            Files.writeString(
                    tempFile, json, StandardCharsets.UTF_8
            );

            if (Files.exists(target)) {
                Files.copy(
                        target,
                        getBackupPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            replaceFile(tempFile, target);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    public AppData loadAllData() throws IOException {
        return readData(filePath);
    }

    public void restoreFromBackup() throws IOException {
        Path backup = getBackupPath();

        // Check the backup before replacing the current file.
        readData(backup);

        Path target = filePath.toAbsolutePath();
        Path parent = target.getParent();

        Files.createDirectories(parent);

        Path tempFile = Files.createTempFile(
                parent, "coursecompass-restore-", ".tmp"
        );

        try {
            Files.copy(
                    backup,
                    tempFile,
                    StandardCopyOption.REPLACE_EXISTING
            );

            replaceFile(tempFile, target);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path getBackupPath() {
        Path target = filePath.toAbsolutePath();

        return target.resolveSibling(
                target.getFileName().toString() + ".bak"
        );
    }

    private AppData readData(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("No saved data found");
        }

        try {
            String json = Files.readString(
                    path, StandardCharsets.UTF_8
            );

            AppData data = gson.fromJson(json, AppData.class);

            if (data == null
                    || data.getAssignments() == null
                    || data.getStepsByAssignment() == null) {
                throw new IOException("Invalid saved data");
            }

            return data;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Could not read saved data", e);
        }
    }

    private void replaceFile(Path source, Path target)
            throws IOException {
        try {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(
                    source,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}


package compass.storage;

import compass.model.Assignment;

import java.io.IOException;
import java.util.List;

public class StorageWorkflow {

    private final LocalStorageService storage;
    private final AssignmentMergeService merger;

    public StorageWorkflow(LocalStorageService storage) {
        if (storage == null) {
            throw new IllegalArgumentException(
                    "Storage cannot be null"
            );
        }

        this.storage = storage;
        this.merger = new AssignmentMergeService();
    }

    public AppData mergeAndSave(
            AppData currentData,
            List<Assignment> importedAssignments,
            boolean replaceMode
    ) throws IOException {

        AppData merged = merger.mergeImportedAssignments(
                currentData,
                importedAssignments,
                replaceMode
        );

        storage.saveAllData(merged);

        return merged;
    }
}

package org.dragon.memory.qmd;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySyncProgressUpdate;
import org.dragon.memory.models.SyncRequest;

public class QmdUpdateCoordinator {

    private final ResolvedMemorySearchConfig searchConfig;
    private final QmdCliClient qmdCliClient;

    public QmdUpdateCoordinator(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.qmdCliClient = new QmdCliClient();
    }

    public MemorySyncProgressUpdate sync(SyncRequest request) {
        MemorySyncProgressUpdate progress = new MemorySyncProgressUpdate();
        progress.setStatus("syncing");

        // 执行 qmd update
        String[] updateArgs = {"update"};
        String updateOutput = qmdCliClient.executeCommand("qmd", updateArgs);
        if (updateOutput.contains("Error")) {
            progress.setError(updateOutput);
            progress.setStatus("failed");
            return progress;
        }

        // 如果需要，执行 qmd embed
        if (searchConfig.isStoreVectorEnabled()) {
            String[] embedArgs = {"embed"};
            String embedOutput = qmdCliClient.executeCommand("qmd", embedArgs);
            if (embedOutput.contains("Error")) {
                progress.setError(embedOutput);
                progress.setStatus("failed");
                return progress;
            }
        }

        progress.setStatus("complete");
        return progress;
    }
}
package org.dragon.memory.command;

import org.dragon.memory.entity.MemoryEntry;

/**
 * 保存工作空间记忆命令
 *
 * @author binarytom
 * @version 1.0
 */
public class SaveWorkspaceMemoryCommand {
    private final String workspaceId;
    private final MemoryEntry memoryEntry;

    private SaveWorkspaceMemoryCommand(Builder builder) {
        this.workspaceId = builder.workspaceId;
        this.memoryEntry = builder.memoryEntry;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public MemoryEntry getMemoryEntry() {
        return memoryEntry;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String workspaceId;
        private MemoryEntry memoryEntry;

        public Builder workspaceId(String workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder memoryEntry(MemoryEntry memoryEntry) {
            this.memoryEntry = memoryEntry;
            return this;
        }

        public SaveWorkspaceMemoryCommand build() {
            return new SaveWorkspaceMemoryCommand(this);
        }
    }
}

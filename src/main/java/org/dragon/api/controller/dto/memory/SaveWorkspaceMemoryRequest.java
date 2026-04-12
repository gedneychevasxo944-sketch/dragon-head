package org.dragon.api.controller.dto.memory;

import org.dragon.memory.entity.MemoryEntry;

/**
 * 保存工作空间记忆请求DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class SaveWorkspaceMemoryRequest {
    private String workspaceId;
    private MemoryEntry memoryEntry;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public MemoryEntry getMemoryEntry() {
        return memoryEntry;
    }

    public void setMemoryEntry(MemoryEntry memoryEntry) {
        this.memoryEntry = memoryEntry;
    }
}

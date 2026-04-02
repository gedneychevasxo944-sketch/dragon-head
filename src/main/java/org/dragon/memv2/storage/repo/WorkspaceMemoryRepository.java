package org.dragon.memv2.storage.repo;

import org.dragon.memv2.core.MemoryEntry;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间记忆仓库接口
 * 负责管理工作空间共享记忆的增删改查和索引维护
 *
 * @author binarytom
 * @version 1.0
 */
public interface WorkspaceMemoryRepository {
    MemoryEntry create(String workspaceId, MemoryEntry entry);

    MemoryEntry update(String workspaceId, MemoryEntry entry);

    Optional<MemoryEntry> get(String workspaceId, String memoryId);

    List<MemoryEntry> list(String workspaceId);

    void delete(String workspaceId, String memoryId);

    void rebuildIndex(String workspaceId);
}

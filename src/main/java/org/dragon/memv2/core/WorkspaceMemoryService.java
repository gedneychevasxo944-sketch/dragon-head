package org.dragon.memv2.core;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间记忆服务接口
 * 负责管理工作空间共享记忆的增删改查和索引维护
 *
 * @author binarytom
 * @version 1.0
 */
public interface WorkspaceMemoryService {
    MemoryEntry create(String workspaceId, MemoryEntry entry);

    MemoryEntry update(String workspaceId, MemoryEntry entry);

    Optional<MemoryEntry> get(String workspaceId, MemoryId memoryId);

    List<MemoryEntry> list(String workspaceId);

    void delete(String workspaceId, MemoryId memoryId);

    void rebuildIndex(String workspaceId);
}

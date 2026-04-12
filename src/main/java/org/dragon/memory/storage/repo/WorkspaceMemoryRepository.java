package org.dragon.memory.storage.repo;
import org.dragon.memory.entity.MemoryId;


import org.dragon.memory.entity.MemoryEntry;

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

    Optional<MemoryEntry> get(String workspaceId, MemoryId memoryId);

    List<MemoryEntry> list(String workspaceId);

    void delete(String workspaceId, MemoryId memoryId);

    void rebuildIndex(String workspaceId);

    /**
     * 初始化workSpace记忆空间，创建目录结构和初始文件
     */
    void initSpace(String workspaceId);
}

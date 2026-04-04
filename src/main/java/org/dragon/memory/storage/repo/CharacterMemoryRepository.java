package org.dragon.memory.storage.repo;
import org.dragon.memory.core.MemoryId;


import org.dragon.memory.core.MemoryEntry;

import java.util.List;
import java.util.Optional;

/**
 * 角色记忆仓库接口
 * 负责管理角色长期记忆的增删改查和索引维护
 *
 * @author binarytom
 * @version 1.0
 */
public interface CharacterMemoryRepository {
    MemoryEntry create(String characterId, MemoryEntry entry);

    MemoryEntry update(String characterId, MemoryEntry entry);

    Optional<MemoryEntry> get(String characterId, MemoryId memoryId);

    List<MemoryEntry> list(String characterId);

    void delete(String characterId, MemoryId memoryId);

    void rebuildIndex(String characterId);
}

package org.dragon.memory.service.core;

import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.entity.MemoryId;

import java.util.List;
import java.util.Optional;

/**
 * 角色记忆服务接口
 * 负责管理角色长期记忆的增删改查和索引维护
 *
 * @author binarytom
 * @version 1.0
 */
public interface CharacterMemoryService {
    MemoryEntry create(String characterId, MemoryEntry entry);

    MemoryEntry update(String characterId, MemoryEntry entry);

    Optional<MemoryEntry> get(String characterId, MemoryId memoryId);

    List<MemoryEntry> list(String characterId);

    void delete(String characterId, MemoryId memoryId);

    void rebuildIndex(String characterId);
}

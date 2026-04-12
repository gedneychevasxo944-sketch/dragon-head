package org.dragon.memory.service.core.impl;
import org.dragon.memory.entity.MemoryId;


import org.dragon.memory.entity.MemoryEntry;
import org.dragon.memory.service.core.CharacterMemoryService;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 角色记忆服务实现类
 * 负责管理角色长期记忆的增删改查和索引维护
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class CharacterMemoryServiceImpl implements CharacterMemoryService {
    private final CharacterMemoryRepository characterMemoryRepository;

    public CharacterMemoryServiceImpl(CharacterMemoryRepository characterMemoryRepository) {
        this.characterMemoryRepository = characterMemoryRepository;
    }

    @Override
    public MemoryEntry create(String characterId, MemoryEntry entry) {
        return characterMemoryRepository.create(characterId, entry);
    }

    @Override
    public MemoryEntry update(String characterId, MemoryEntry entry) {
        return characterMemoryRepository.update(characterId, entry);
    }

    @Override
    public Optional<MemoryEntry> get(String characterId, MemoryId memoryId) {
        return characterMemoryRepository.get(characterId, memoryId);
    }

    @Override
    public List<MemoryEntry> list(String characterId) {
        return characterMemoryRepository.list(characterId);
    }

    @Override
    public void delete(String characterId, MemoryId memoryId) {
        characterMemoryRepository.delete(characterId, memoryId);
    }

    @Override
    public void rebuildIndex(String characterId) {
        characterMemoryRepository.rebuildIndex(characterId);
    }
}

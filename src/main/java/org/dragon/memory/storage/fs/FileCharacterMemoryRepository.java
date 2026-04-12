package org.dragon.memory.storage.fs;
import org.dragon.memory.core.MemoryId;


import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.storage.MemoryPathResolver;
import org.dragon.memory.storage.MemoryMarkdownParser;
import org.dragon.memory.storage.MemoryIndexParser;
import org.dragon.memory.storage.repo.CharacterMemoryRepository;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 文件系统角色记忆仓库实现类
 * 使用本地文件系统存储角色记忆，管理 MEMORY.md 索引和 mem 目录下的记忆文件
 *
 * @author binarytom
 * @version 1.0
 */
@Repository
public class FileCharacterMemoryRepository extends AbstractFileMemoryRepository implements CharacterMemoryRepository {

    public FileCharacterMemoryRepository(MemoryPathResolver pathResolver,
                                         MemoryMarkdownParser markdownParser,
                                         MemoryIndexParser indexParser) {
        super(pathResolver, markdownParser, indexParser);
    }

    @Override
    protected Path resolveMemDir(String characterId) {
        return pathResolver.resolveCharacterMemDir(characterId);
    }

    @Override
    protected Path resolveIndexPath(String characterId) {
        return pathResolver.resolveCharacterIndex(characterId);
    }

    @Override
    protected Path resolveRootPath(String characterId) {
        return pathResolver.resolveCharacterRoot(characterId);
    }

    @Override
    protected Path resolveBindingsPath(String characterId) {
        return pathResolver.resolveCharacterBindings(characterId);
    }

    @Override
    public MemoryEntry create(String characterId, MemoryEntry entry) {
        return super.createMemory(characterId, entry);
    }

    @Override
    public MemoryEntry update(String characterId, MemoryEntry entry) {
        return super.updateMemory(characterId, entry);
    }

    @Override
    public Optional<MemoryEntry> get(String characterId, MemoryId memoryId) {
        return super.getMemory(characterId, memoryId);
    }

    @Override
    public List<MemoryEntry> list(String characterId) {
        return super.listMemories(characterId);
    }

    @Override
    public void delete(String characterId, MemoryId memoryId) {
        super.deleteMemory(characterId, memoryId);
    }

    @Override
    public void rebuildIndex(String characterId) {
        super.rebuildIndex(characterId);
    }

    @Override
    public void initSpace(String characterId) {
        super.initSpace(characterId);
    }
}
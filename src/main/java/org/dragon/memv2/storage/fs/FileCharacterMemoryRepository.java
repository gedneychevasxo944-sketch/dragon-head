package org.dragon.memv2.storage.fs;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.storage.MemoryPathResolver;
import org.dragon.memv2.storage.MemoryMarkdownParser;
import org.dragon.memv2.storage.MemoryIndexParser;
import org.dragon.memv2.storage.repo.CharacterMemoryRepository;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 文件系统角色记忆仓库实现类
 * 使用本地文件系统存储角色记忆，管理 MEMORY.md 索引和 mem 目录下的记忆文件
 *
 * @author wyj
 * @version 1.0
 */
@Repository
public class FileCharacterMemoryRepository implements CharacterMemoryRepository {
    private final MemoryPathResolver pathResolver;
    private final MemoryMarkdownParser markdownParser;
    private final MemoryIndexParser indexParser;

    public FileCharacterMemoryRepository(MemoryPathResolver pathResolver,
                                         MemoryMarkdownParser markdownParser,
                                         MemoryIndexParser indexParser) {
        this.pathResolver = pathResolver;
        this.markdownParser = markdownParser;
        this.indexParser = indexParser;
    }

    @Override
    public MemoryEntry create(String characterId, MemoryEntry entry) {
        Path memDir = pathResolver.resolveCharacterMemDir(characterId);
        try {
            if (!Files.exists(memDir)) {
                Files.createDirectories(memDir);
            }
            Path filePath = memDir.resolve(entry.getFileName());
            Files.writeString(filePath, markdownParser.render(entry));
            rebuildIndex(characterId);
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create character memory: " + e.getMessage(), e);
        }
    }

    @Override
    public MemoryEntry update(String characterId, MemoryEntry entry) {
        Path memDir = pathResolver.resolveCharacterMemDir(characterId);
        try {
            Path filePath = memDir.resolve(entry.getFileName());
            Files.writeString(filePath, markdownParser.render(entry));
            rebuildIndex(characterId);
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update character memory: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<MemoryEntry> get(String characterId, String memoryId) {
        Path memDir = pathResolver.resolveCharacterMemDir(characterId);
        try {
            if (!Files.exists(memDir)) {
                return Optional.empty();
            }
            // 简化实现：实际应根据ID查找对应文件
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get character memory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MemoryEntry> list(String characterId) {
        Path memDir = pathResolver.resolveCharacterMemDir(characterId);
        try {
            if (!Files.exists(memDir)) {
                return List.of();
            }
            // 简化实现：实际应列出所有记忆文件
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list character memories: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String characterId, String memoryId) {
        // 简化实现：实际应删除对应文件并更新索引
    }

    @Override
    public void rebuildIndex(String characterId) {
        Path indexPath = pathResolver.resolveCharacterIndex(characterId);
        try {
            if (!Files.exists(indexPath.getParent())) {
                Files.createDirectories(indexPath.getParent());
            }
            // 简化实现：实际应根据当前记忆条目重建索引
            Files.writeString(indexPath, indexParser.render(List.of()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to rebuild character index: " + e.getMessage(), e);
        }
    }
}

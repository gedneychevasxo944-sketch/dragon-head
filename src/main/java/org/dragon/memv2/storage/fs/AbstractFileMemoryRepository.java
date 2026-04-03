package org.dragon.memv2.storage.fs;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.MemoryIndexItem;
import org.dragon.memv2.storage.MemoryMarkdownParser;
import org.dragon.memv2.storage.MemoryIndexParser;
import org.dragon.memv2.storage.MemoryPathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 文件系统记忆仓库抽象基类
 * 沉淀文件存储的工具能力，提供统一的文件操作和索引管理
 *
 * @author binarytom
 * @version 1.0
 */
public abstract class AbstractFileMemoryRepository {
    protected final MemoryPathResolver pathResolver;
    protected final MemoryMarkdownParser markdownParser;
    protected final MemoryIndexParser indexParser;

    protected AbstractFileMemoryRepository(MemoryPathResolver pathResolver,
                                          MemoryMarkdownParser markdownParser,
                                          MemoryIndexParser indexParser) {
        this.pathResolver = pathResolver;
        this.markdownParser = markdownParser;
        this.indexParser = indexParser;
    }

    /**
     * 解析记忆文件目录
     *
     * @param id 角色ID或工作空间ID
     * @return 记忆文件目录路径
     */
    protected abstract Path resolveMemDir(String id);

    /**
     * 解析索引文件路径
     *
     * @param id 角色ID或工作空间ID
     * @return 索引文件路径
     */
    protected abstract Path resolveIndexPath(String id);

    /**
     * 创建记忆文件
     */
    protected MemoryEntry createMemory(String id, MemoryEntry entry) {
        Path memDir = resolveMemDir(id);
        try {
            if (!Files.exists(memDir)) {
                Files.createDirectories(memDir);
            }
            Path filePath = memDir.resolve(entry.getFileName());
            Files.writeString(filePath, markdownParser.render(entry));
            rebuildIndex(id);
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create memory: " + e.getMessage(), e);
        }
    }

    /**
     * 更新记忆文件
     */
    protected MemoryEntry updateMemory(String id, MemoryEntry entry) {
        Path memDir = resolveMemDir(id);
        try {
            Path filePath = memDir.resolve(entry.getFileName());
            Files.writeString(filePath, markdownParser.render(entry));
            rebuildIndex(id);
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update memory: " + e.getMessage(), e);
        }
    }

    /**
     * 获取记忆文件
     */
    protected Optional<MemoryEntry> getMemory(String id, MemoryId memoryId) {
        Path memDir = resolveMemDir(id);
        try {
            if (!Files.exists(memDir)) {
                return Optional.empty();
            }

            try (Stream<Path> paths = Files.list(memDir)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .map(this::readMemoryFile)
                        .filter(entry -> entry.getId() != null && memoryId.equals(entry.getId()))
                        .findFirst();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get memory: " + e.getMessage(), e);
        }
    }

    /**
     * 列出所有记忆文件
     */
    protected List<MemoryEntry> listMemories(String id) {
        Path memDir = resolveMemDir(id);
        List<MemoryEntry> entries = new ArrayList<>();
        try {
            if (!Files.exists(memDir)) {
                return entries;
            }

            try (Stream<Path> paths = Files.list(memDir)) {
                paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .map(this::readMemoryFile)
                        .forEach(entries::add);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to list memories: " + e.getMessage(), e);
        }
        return entries;
    }

    /**
     * 删除记忆文件
     */
    protected void deleteMemory(String id, MemoryId memoryId) {
        Path memDir = resolveMemDir(id);
        try {
            Optional<MemoryEntry> entryOpt = getMemory(id, memoryId);
            if (entryOpt.isPresent()) {
                Path filePath = memDir.resolve(entryOpt.get().getFileName());
                Files.deleteIfExists(filePath);
                rebuildIndex(id);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete memory: " + e.getMessage(), e);
        }
    }

    /**
     * 重建索引
     */
    protected void rebuildIndex(String id) {
        Path indexPath = resolveIndexPath(id);
        try {
            if (!Files.exists(indexPath.getParent())) {
                Files.createDirectories(indexPath.getParent());
            }

            List<MemoryEntry> entries = listMemories(id);
            Files.writeString(indexPath, indexParser.render(convertToIndexItems(entries)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to rebuild index: " + e.getMessage(), e);
        }
    }

    /**
     * 读取记忆文件
     */
    protected MemoryEntry readMemoryFile(Path path) {
        try {
            String content = Files.readString(path);
            MemoryEntry entry = markdownParser.parse(content);
            entry.setFileName(path.getFileName().toString());
            entry.setFilePath(path.toString());
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read memory file: " + path, e);
        }
    }

    /**
     * 将 MemoryEntry 转换为 MemoryIndexItem
     */
    protected List<MemoryIndexItem> convertToIndexItems(List<MemoryEntry> entries) {
        List<MemoryIndexItem> items = new ArrayList<>();
        for (MemoryEntry entry : entries) {
            MemoryIndexItem item = new MemoryIndexItem();
            item.setMemoryId(entry.getId());
            item.setTitle(entry.getTitle());
            item.setRelativePath(entry.getFileName());
            item.setSummaryLine(entry.getDescription());
            item.setType(entry.getType());
            item.setUpdatedAt(entry.getUpdatedAt());
            items.add(item);
        }
        return items;
    }
}

package org.dragon.memv2.storage.fs;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.storage.MemoryPathResolver;
import org.dragon.memv2.storage.MemoryMarkdownParser;
import org.dragon.memv2.storage.MemoryIndexParser;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 文件系统工作空间记忆仓库实现类
 * 使用本地文件系统存储工作空间记忆，管理 MEMORY.md 索引和 mem 目录下的记忆文件
 *
 * @author binarytom
 * @version 1.0
 */
@Repository
public class FileWorkspaceMemoryRepository implements WorkspaceMemoryRepository {
    private final MemoryPathResolver pathResolver;
    private final MemoryMarkdownParser markdownParser;
    private final MemoryIndexParser indexParser;

    public FileWorkspaceMemoryRepository(MemoryPathResolver pathResolver,
                                         MemoryMarkdownParser markdownParser,
                                         MemoryIndexParser indexParser) {
        this.pathResolver = pathResolver;
        this.markdownParser = markdownParser;
        this.indexParser = indexParser;
    }

    @Override
    public MemoryEntry create(String workspaceId, MemoryEntry entry) {
        Path memDir = pathResolver.resolveWorkspaceMemDir(workspaceId);
        try {
            if (!Files.exists(memDir)) {
                Files.createDirectories(memDir);
            }
            Path filePath = memDir.resolve(entry.getFileName());
            Files.writeString(filePath, markdownParser.render(entry));
            rebuildIndex(workspaceId);
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create workspace memory: " + e.getMessage(), e);
        }
    }

    @Override
    public MemoryEntry update(String workspaceId, MemoryEntry entry) {
        Path memDir = pathResolver.resolveWorkspaceMemDir(workspaceId);
        try {
            Path filePath = memDir.resolve(entry.getFileName());
            Files.writeString(filePath, markdownParser.render(entry));
            rebuildIndex(workspaceId);
            return entry;
        } catch (IOException e) {
            throw new RuntimeException("Failed to update workspace memory: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<MemoryEntry> get(String workspaceId, String memoryId) {
        Path memDir = pathResolver.resolveWorkspaceMemDir(workspaceId);
        try {
            if (!Files.exists(memDir)) {
                return Optional.empty();
            }

            try (Stream<Path> paths = Files.list(memDir)) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".md"))
                        .map(this::readMemoryFile)
                        .filter(entry -> memoryId.equals(entry.getId()))
                        .findFirst();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get workspace memory: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MemoryEntry> list(String workspaceId) {
        Path memDir = pathResolver.resolveWorkspaceMemDir(workspaceId);
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
            throw new RuntimeException("Failed to list workspace memories: " + e.getMessage(), e);
        }
        return entries;
    }

    @Override
    public void delete(String workspaceId, String memoryId) {
        Path memDir = pathResolver.resolveWorkspaceMemDir(workspaceId);
        try {
            Optional<MemoryEntry> entryOpt = get(workspaceId, memoryId);
            if (entryOpt.isPresent()) {
                Path filePath = memDir.resolve(entryOpt.get().getFileName());
                Files.deleteIfExists(filePath);
                rebuildIndex(workspaceId);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete workspace memory: " + e.getMessage(), e);
        }
    }

    @Override
    public void rebuildIndex(String workspaceId) {
        Path indexPath = pathResolver.resolveWorkspaceIndex(workspaceId);
        try {
            if (!Files.exists(indexPath.getParent())) {
                Files.createDirectories(indexPath.getParent());
            }

            List<MemoryEntry> entries = list(workspaceId);
            Files.writeString(indexPath, indexParser.render(convertToIndexItems(entries)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to rebuild workspace index: " + e.getMessage(), e);
        }
    }

    /**
     * 读取记忆文件
     */
    private MemoryEntry readMemoryFile(Path path) {
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
    private List<org.dragon.memv2.core.MemoryIndexItem> convertToIndexItems(List<MemoryEntry> entries) {
        List<org.dragon.memv2.core.MemoryIndexItem> items = new ArrayList<>();
        for (MemoryEntry entry : entries) {
            org.dragon.memv2.core.MemoryIndexItem item = new org.dragon.memv2.core.MemoryIndexItem();
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

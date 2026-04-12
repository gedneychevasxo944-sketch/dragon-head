package org.dragon.memory.storage.fs;
import org.dragon.memory.core.MemoryId;


import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.storage.MemoryPathResolver;
import org.dragon.memory.storage.MemoryMarkdownParser;
import org.dragon.memory.storage.MemoryIndexParser;
import org.dragon.memory.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 文件系统工作空间记忆仓库实现类
 * 使用本地文件系统存储工作空间记忆，管理 MEMORY.md 索引和 mem 目录下的记忆文件
 *
 * @author binarytom
 * @version 1.0
 */
@Repository
public class FileWorkspaceMemoryRepository extends AbstractFileMemoryRepository implements WorkspaceMemoryRepository {

    public FileWorkspaceMemoryRepository(MemoryPathResolver pathResolver,
                                         MemoryMarkdownParser markdownParser,
                                         MemoryIndexParser indexParser) {
        super(pathResolver, markdownParser, indexParser);
    }

    @Override
    protected Path resolveMemDir(String workspaceId) {
        return pathResolver.resolveWorkspaceMemDir(workspaceId);
    }

    @Override
    protected Path resolveIndexPath(String workspaceId) {
        return pathResolver.resolveWorkspaceIndex(workspaceId);
    }

    @Override
    protected Path resolveRootPath(String workspaceId) {
        return pathResolver.resolveWorkspaceRoot(workspaceId);
    }

    @Override
    protected Path resolveBindingsPath(String workspaceId) {
        return pathResolver.resolveWorkspaceBindings(workspaceId);
    }

    @Override
    public MemoryEntry create(String workspaceId, MemoryEntry entry) {
        return super.createMemory(workspaceId, entry);
    }

    @Override
    public MemoryEntry update(String workspaceId, MemoryEntry entry) {
        return super.updateMemory(workspaceId, entry);
    }

    @Override
    public Optional<MemoryEntry> get(String workspaceId, MemoryId memoryId) {
        return super.getMemory(workspaceId, memoryId);
    }

    @Override
    public List<MemoryEntry> list(String workspaceId) {
        return super.listMemories(workspaceId);
    }

    @Override
    public void delete(String workspaceId, MemoryId memoryId) {
        super.deleteMemory(workspaceId, memoryId);
    }

    @Override
    public void rebuildIndex(String workspaceId) {
        super.rebuildIndex(workspaceId);
    }

    @Override
    public void initSpace(String workspaceId) {
        super.initSpace(workspaceId);
    }
}
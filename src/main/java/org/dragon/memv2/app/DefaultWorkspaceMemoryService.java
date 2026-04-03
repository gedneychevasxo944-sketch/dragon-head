package org.dragon.memv2.app;

import org.dragon.memv2.core.MemoryEntry;
import org.dragon.memv2.core.WorkspaceMemoryService;
import org.dragon.memv2.storage.repo.WorkspaceMemoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间记忆服务实现类
 * 负责管理工作空间共享记忆的增删改查和索引维护
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultWorkspaceMemoryService implements WorkspaceMemoryService {
    private final WorkspaceMemoryRepository workspaceMemoryRepository;

    public DefaultWorkspaceMemoryService(WorkspaceMemoryRepository workspaceMemoryRepository) {
        this.workspaceMemoryRepository = workspaceMemoryRepository;
    }

    @Override
    public MemoryEntry create(String workspaceId, MemoryEntry entry) {
        return workspaceMemoryRepository.create(workspaceId, entry);
    }

    @Override
    public MemoryEntry update(String workspaceId, MemoryEntry entry) {
        return workspaceMemoryRepository.update(workspaceId, entry);
    }

    @Override
    public Optional<MemoryEntry> get(String workspaceId, MemoryId memoryId) {
        return workspaceMemoryRepository.get(workspaceId, memoryId);
    }

    @Override
    public List<MemoryEntry> list(String workspaceId) {
        return workspaceMemoryRepository.list(workspaceId);
    }

    @Override
    public void delete(String workspaceId, MemoryId memoryId) {
        workspaceMemoryRepository.delete(workspaceId, memoryId);
    }

    @Override
    public void rebuildIndex(String workspaceId) {
        workspaceMemoryRepository.rebuildIndex(workspaceId);
    }
}

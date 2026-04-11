package org.dragon.workspace.store;

import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.dragon.workspace.Workspace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * MemoryWorkspaceStore 工作空间内存存储实现
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@StoreTypeAnn(StoreType.MEMORY)
public class MemoryWorkspaceStore implements WorkspaceStore {

    private final Map<String, Workspace> store = new ConcurrentHashMap<>();

    @Override
    public void save(Workspace workspace) {
        store.put(workspace.getId(), workspace);
        log.debug("[MemoryWorkspaceStore] Saved workspace: {}", workspace.getId());
    }

    @Override
    public void update(Workspace workspace) {
        if (store.containsKey(workspace.getId())) {
            store.put(workspace.getId(), workspace);
            log.debug("[MemoryWorkspaceStore] Updated workspace: {}", workspace.getId());
        }
    }

    @Override
    public void delete(String id) {
        store.remove(id);
        log.debug("[MemoryWorkspaceStore] Deleted workspace: {}", id);
    }

    @Override
    public Optional<Workspace> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Workspace> findAll() {
        return store.values().stream().collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByStatus(Workspace.Status status) {
        return store.values().stream()
                .filter(ws -> ws.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByOwner(String owner) {
        return store.values().stream()
                .filter(ws -> owner.equals(ws.getOwner()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByIds(List<String> ids) {
        return store.values().stream()
                .filter(ws -> ids.contains(ws.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Workspace> findByIdsAndStatus(List<String> ids, Workspace.Status status) {
        return store.values().stream()
                .filter(ws -> ids.contains(ws.getId()) && ws.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(String id) {
        return store.containsKey(id);
    }

    @Override
    public int count() {
        return store.size();
    }
}

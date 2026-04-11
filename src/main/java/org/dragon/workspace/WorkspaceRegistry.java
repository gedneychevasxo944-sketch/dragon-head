package org.dragon.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dragon.store.StoreFactory;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.store.WorkspaceStore;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceRegistry 工作空间注册中心
 * 负责管理所有 Workspace 的生命周期
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class WorkspaceRegistry {

    private final WorkspaceStore workspaceStore;

    /**
     * 默认 Workspace ID
     */
    private volatile String defaultWorkspaceId;

    public WorkspaceRegistry(StoreFactory storeFactory) {
        this.workspaceStore = storeFactory.get(WorkspaceStore.class);
    }

    /**
     * 注册 Workspace
     *
     * @param workspace Workspace 实例
     * @return 注册后的 Workspace（含 ID）
     */
    public Workspace register(Workspace workspace) {
        if (workspace == null) {
            throw new IllegalArgumentException("Workspace cannot be null");
        }

        // 生成 ID 如果没有提供
        if (workspace.getId() == null || workspace.getId().isEmpty()) {
            workspace.setId(UUID.randomUUID().toString());
        }

        // 设置创建/更新时间
        if (workspace.getCreatedAt() == null) {
            workspace.setCreatedAt(LocalDateTime.now());
        }
        workspace.setUpdatedAt(LocalDateTime.now());

        // 如果是第一个 Workspace，设为默认
        if (workspaceStore.count() == 0) {
            defaultWorkspaceId = workspace.getId();
        }

        workspaceStore.save(workspace);
        log.info("[WorkspaceRegistry] Registered workspace: {}", workspace.getId());

        return workspace;
    }

    /**
     * 注销 Workspace
     *
     * @param workspaceId Workspace ID
     */
    public void unregister(String workspaceId) {
        workspaceStore.delete(workspaceId);

        // 如果删除的是默认 Workspace，选择下一个
        if (defaultWorkspaceId != null && defaultWorkspaceId.equals(workspaceId)) {
            List<Workspace> all = workspaceStore.findAll();
            defaultWorkspaceId = all.isEmpty() ? null : all.get(0).getId();
        }

        log.info("[WorkspaceRegistry] Unregistered workspace: {}", workspaceId);
    }

    /**
     * 获取 Workspace
     *
     * @param workspaceId Workspace ID
     * @return Optional Workspace
     */
    public Optional<Workspace> get(String workspaceId) {
        return workspaceStore.findById(workspaceId);
    }

    /**
     * 获取默认 Workspace
     *
     * @return Optional Workspace
     */
    public Optional<Workspace> getDefaultWorkspace() {
        if (defaultWorkspaceId == null) {
            return Optional.empty();
        }
        return get(defaultWorkspaceId);
    }

    /**
     * 获取所有 Workspace
     *
     * @return Workspace 列表
     */
    public List<Workspace> listAll() {
        return workspaceStore.findAll();
    }

    /**
     * 根据状态获取 Workspace 列表
     *
     * @param status 工作空间状态
     * @return Workspace 列表
     */
    public List<Workspace> listByStatus(Workspace.Status status) {
        return workspaceStore.findByStatus(status);
    }

    /**
     * 根据所有者获取 Workspace 列表
     *
     * @param owner 所有者 ID
     * @return Workspace 列表
     */
    public List<Workspace> listByOwner(String owner) {
        return workspaceStore.findByOwner(owner);
    }

    /**
     * 更新 Workspace
     *
     * @param workspace Workspace 实例
     */
    public void update(Workspace workspace) {
        if (workspace == null || workspace.getId() == null) {
            throw new IllegalArgumentException("Workspace or Workspace id cannot be null");
        }

        if (!workspaceStore.exists(workspace.getId())) {
            throw new IllegalArgumentException("Workspace not found: " + workspace.getId());
        }

        workspace.setUpdatedAt(LocalDateTime.now());
        workspaceStore.update(workspace);
        log.info("[WorkspaceRegistry] Updated workspace: {}", workspace.getId());
    }

    /**
     * 设置默认 Workspace
     *
     * @param workspaceId Workspace ID
     */
    public void setDefaultWorkspace(String workspaceId) {
        if (!workspaceStore.exists(workspaceId)) {
            throw new IllegalArgumentException("Workspace not found: " + workspaceId);
        }
        defaultWorkspaceId = workspaceId;
        log.info("[WorkspaceRegistry] Set default workspace: {}", workspaceId);
    }

    /**
     * 激活 Workspace
     *
     * @param workspaceId Workspace ID
     */
    public void activate(String workspaceId) {
        get(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(Workspace.Status.ACTIVE);
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.update(workspace);
            log.info("[WorkspaceRegistry] Activated workspace: {}", workspaceId);
        });
    }

    /**
     * 停用 Workspace
     *
     * @param workspaceId Workspace ID
     */
    public void deactivate(String workspaceId) {
        get(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(Workspace.Status.INACTIVE);
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.update(workspace);
            log.info("[WorkspaceRegistry] Deactivated workspace: {}", workspaceId);
        });
    }

    /**
     * 归档 Workspace
     *
     * @param workspaceId Workspace ID
     */
    public void archive(String workspaceId) {
        get(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(Workspace.Status.ARCHIVED);
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.update(workspace);
            log.info("[WorkspaceRegistry] Archived workspace: {}", workspaceId);
        });
    }

    /**
     * 获取注册表大小
     *
     * @return 注册的 Workspace 数量
     */
    public int size() {
        return workspaceStore.count();
    }

    /**
     * 检查 Workspace 是否存在
     *
     * @param workspaceId Workspace ID
     * @return 是否存在
     */
    public boolean exists(String workspaceId) {
        return workspaceStore.exists(workspaceId);
    }
}

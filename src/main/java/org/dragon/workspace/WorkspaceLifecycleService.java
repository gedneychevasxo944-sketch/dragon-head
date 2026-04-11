package org.dragon.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dragon.permission.enums.ResourceType;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.util.UserUtils;
import org.dragon.workspace.Workspace;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceLifecycleService 工作空间生命周期服务
 * 管理工作空间的创建、更新、删除、激活、停用、归档等
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceLifecycleService {

    private final WorkspaceRegistry workspaceRegistry;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;

    /**
     * 创建工作空间
     *
     * @param workspace 工作空间
     * @return 创建后的工作空间
     */
    public Workspace createWorkspace(Workspace workspace) {
        if (workspace.getId() == null || workspace.getId().isEmpty()) {
            workspace.setId(UUID.randomUUID().toString());
        }
        if (StringUtils.isBlank(workspace.getOwner())) {
            workspace.setOwner(UserUtils.getUserId());
        }
        workspace.setCreatedAt(LocalDateTime.now());
        workspace.setUpdatedAt(LocalDateTime.now());

        if (workspace.getStatus() == null) {
            workspace.setStatus(Workspace.Status.INACTIVE);
        }

        workspaceRegistry.register(workspace);

        // 添加创建者为 Owner
        Long ownerId = Long.parseLong(String.valueOf(workspace.getOwner()));
        assetMemberService.addOwnerDirectly(ResourceType.WORKSPACE, workspace.getId(), ownerId);

        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.WORKSPACE, workspace.getId(), String.valueOf(ownerId));

        log.info("[WorkspaceLifecycleService] Created workspace: {}", workspace.getId());

        return workspace;
    }

    /**
     * 更新工作空间
     * <p>
     * 只更新传入 workspace 中非 null 的属性，保留数据库中已存在的其他属性
     *
     * @param workspace 工作空间（只更新非 null 属性）
     * @return 更新后的工作空间
     */
    public Workspace updateWorkspace(Workspace workspace) {
        Workspace existing = workspaceRegistry.get(workspace.getId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspace.getId()));

        // 只更新非 null 属性
        if (workspace.getName() != null) {
            existing.setName(workspace.getName());
        }
        if (workspace.getDescription() != null) {
            existing.setDescription(workspace.getDescription());
        }
        if (workspace.getStatus() != null) {
            existing.setStatus(workspace.getStatus());
        }
        if (workspace.getProperties() != null) {
            existing.setProperties(workspace.getProperties());
        }
        if (workspace.getPersonality() != null) {
            existing.setPersonality(workspace.getPersonality());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        workspaceRegistry.update(existing);
        log.info("[WorkspaceLifecycleService] Updated workspace: {}", workspace.getId());

        return existing;
    }

    /**
     * 删除工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deleteWorkspace(String workspaceId) {
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        workspaceRegistry.unregister(workspaceId);
        // 删除发布状态
        publishStatusService.deleteStatus(ResourceType.WORKSPACE, workspaceId);
        log.info("[WorkspaceLifecycleService] Deleted workspace: {}", workspaceId);
    }

    /**
     * 获取工作空间
     *
     * @param workspaceId 工作空间 ID
     * @return 工作空间
     */
    public Optional<Workspace> getWorkspace(String workspaceId) {
        return workspaceRegistry.get(workspaceId);
    }

    /**
     * 获取所有工作空间
     *
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspaces() {
        return workspaceRegistry.listAll();
    }

    /**
     * 根据状态获取工作空间
     *
     * @param status 工作空间状态
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspacesByStatus(Workspace.Status status) {
        return workspaceRegistry.listByStatus(status);
    }

    /**
     * 激活工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void activateWorkspace(String workspaceId) {
        workspaceRegistry.activate(workspaceId);
        log.info("[WorkspaceLifecycleService] Activated workspace: {}", workspaceId);
    }

    /**
     * 停用工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deactivateWorkspace(String workspaceId) {
        workspaceRegistry.deactivate(workspaceId);
        log.info("[WorkspaceLifecycleService] Deactivated workspace: {}", workspaceId);
    }

    /**
     * 归档工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void archiveWorkspace(String workspaceId) {
        workspaceRegistry.archive(workspaceId);
        log.info("[WorkspaceLifecycleService] Archived workspace: {}", workspaceId);
    }

    /**
     * 获取活跃工作空间数量
     *
     * @return 活跃工作空间数量
     */
    public long countActiveWorkspaces() {
        return workspaceRegistry.listByStatus(Workspace.Status.ACTIVE).size();
    }
}

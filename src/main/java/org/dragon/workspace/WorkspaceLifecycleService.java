package org.dragon.workspace;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.factory.AssetFactory;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.asset.service.AssetMarkService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.enums.BuiltinType;
import org.dragon.permission.enums.ResourceType;
import org.dragon.util.UserUtils;
import org.dragon.workspace.member.HandlerType;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.dragon.workspace.store.WorkspaceStore;
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

    private final WorkspaceStore workspaceStore;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;
    private final WorkspaceMemberService memberService;
    private final AssetFactory assetFactory;
    private final CharacterRegistry characterRegistry;
    private final AssetAssociationService assetAssociationService;
    private final AssetMarkService assetMarkService;

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

        workspaceStore.save(workspace);

        // 添加创建者为 Owner
        Long ownerId = Long.valueOf(String.valueOf(workspace.getOwner()));
        assetMemberService.addOwnerDirectly(ResourceType.WORKSPACE, workspace.getId(), ownerId);

        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.WORKSPACE, workspace.getId(), String.valueOf(ownerId));

        // 为 Workspace 初始化 Built-in Character（fork 全局 Built-in Character）
        initializeBuiltinCharacters(workspace.getId(), ownerId);

        log.info("[WorkspaceLifecycleService] Created workspace: {}", workspace.getId());

        return workspace;
    }

    /**
     * 为 Workspace 初始化 Built-in Character
     * <p>
     * Fork 全局 Built-in Character 作为 Workspace 的本地副本，
     * 并创建 WorkspaceMember 关联。
     *
     * @param workspaceId Workspace ID
     * @param ownerId Owner 用户 ID
     */
    private void initializeBuiltinCharacters(String workspaceId, Long ownerId) {
        for (BuiltinType builtinType : BuiltinType.values()) {
            String globalCharacterId = builtinType.getId();

            // 获取全局 Built-in Character
            Character globalChar = characterRegistry.get(globalCharacterId).orElse(null);
            if (globalChar == null) {
                log.warn("[WorkspaceLifecycleService] Built-in character not found: {}", globalCharacterId);
                continue;
            }

            // Fork 为 Workspace 本地副本
            Character workspaceChar = assetFactory.copyCharacter(globalChar, ownerId);

            // 重命名：添加 workspace 前缀
            workspaceChar.setName(globalChar.getName() + " (" + workspaceId + ")");
            characterRegistry.update(workspaceChar);

            // 创建 WorkspaceMember 关联
            memberService.addMember(workspaceId, workspaceChar.getId(), "BUILTIN",
                    WorkspaceMember.Layer.MANAGEMENT);

            // 创建 CHARACTER_WORKSPACE 关联
            assetAssociationService.createAssociation(
                    AssociationType.CHARACTER_WORKSPACE,
                    ResourceType.CHARACTER, workspaceChar.getId(),
                    ResourceType.WORKSPACE, workspaceId);

            // 标记为 Builtin
            assetMarkService.markAsBuiltin(ResourceType.CHARACTER, workspaceChar.getId());

            log.info("[WorkspaceLifecycleService] Initialized built-in character {} for workspace {}",
                    globalCharacterId, workspaceId);
        }
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
        Workspace existing = workspaceStore.findById(workspace.getId())
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
        workspaceStore.update(existing);
        log.info("[WorkspaceLifecycleService] Updated workspace: {}", workspace.getId());

        return existing;
    }

    /**
     * 删除工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deleteWorkspace(String workspaceId) {
        workspaceStore.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        workspaceStore.delete(workspaceId);
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
        return workspaceStore.findById(workspaceId);
    }

    /**
     * 获取所有工作空间
     *
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspaces() {
        return workspaceStore.findAll();
    }

    /**
     * 根据状态获取工作空间
     *
     * @param status 工作空间状态
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspacesByStatus(Workspace.Status status) {
        return workspaceStore.findByStatus(status);
    }

    /**
     * 根据 ID 列表获取工作空间
     *
     * @param ids 工作空间 ID 列表
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspacesByIds(List<String> ids) {
        return workspaceStore.findByIds(ids);
    }

    /**
     * 根据 ID 列表和状态获取工作空间
     *
     * @param ids 工作空间 ID 列表
     * @param status 工作空间状态
     * @return 工作空间列表
     */
    public List<Workspace> listWorkspacesByIdsAndStatus(List<String> ids, Workspace.Status status) {
        return workspaceStore.findByIdsAndStatus(ids, status);
    }

    /**
     * 激活工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void activateWorkspace(String workspaceId) {
        workspaceStore.findById(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(Workspace.Status.ACTIVE);
            workspaceStore.update(workspace);
            log.info("[WorkspaceLifecycleService] Activated workspace: {}", workspaceId);
        });
    }

    /**
     * 停用工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void deactivateWorkspace(String workspaceId) {
        workspaceStore.findById(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(Workspace.Status.INACTIVE);
            workspaceStore.update(workspace);
            log.info("[WorkspaceLifecycleService] Deactivated workspace: {}", workspaceId);
        });
    }

    /**
     * 归档工作空间
     *
     * @param workspaceId 工作空间 ID
     */
    public void archiveWorkspace(String workspaceId) {
        workspaceStore.findById(workspaceId).ifPresent(workspace -> {
            workspace.setStatus(Workspace.Status.ARCHIVED);
            workspaceStore.update(workspace);
            log.info("[WorkspaceLifecycleService] Archived workspace: {}", workspaceId);
        });
    }

    /**
     * 获取活跃工作空间数量
     *
     * @return 活跃工作空间数量
     */
    public long countActiveWorkspaces() {
        return workspaceStore.findByStatus(Workspace.Status.ACTIVE).size();
    }
}

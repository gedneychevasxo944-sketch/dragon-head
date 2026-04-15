package org.dragon.workspace.member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceMemberManagementService 工作空间成员管理服务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceMemberService {

    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;
    private final AssetAssociationService assetAssociationService;

    private WorkspaceMemberStore getMemberStore() {
        return storeFactory.get(WorkspaceMemberStore.class);
    }

    /**
     * 添加成员到工作空间
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param role 角色
     * @param layer 层级
     * @return 添加的成员
     */
    public WorkspaceMember addMember(String workspaceId, String characterId,
            String role, WorkspaceMember.Layer layer) {
        // 验证工作空间存在
        Workspace workspace = workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 检查成员是否已存在
        Optional<WorkspaceMember> existing = getMemberStore().findByWorkspaceIdAndCharacterId(
                workspaceId, characterId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Character already exists in workspace: " + characterId);
        }

        // 创建成员
        WorkspaceMember member = WorkspaceMember.builder()
                .id(WorkspaceMember.createId(workspaceId, characterId))
                .workspaceId(workspaceId)
                .characterId(characterId)
                .role(role)
                .layer(layer != null ? layer : WorkspaceMember.Layer.NORMAL)
                .weight(1.0) // TODO: 从 Workspace 获取默认权重
                .priority(0) // TODO: 从 Workspace 获取默认优先级
                .reputation(0)
                .joinAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        getMemberStore().save(member);

        // 同步创建 asset_association 记录
        assetAssociationService.createAssociation(
                AssociationType.CHARACTER_WORKSPACE,
                ResourceType.CHARACTER, characterId,
                ResourceType.WORKSPACE, workspaceId);

        log.info("[WorkspaceMemberManagementService] Added member {} to workspace {}",
                characterId, workspaceId);

        return member;
    }

    /**
     * 从工作空间移除成员
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     */
    public void removeMember(String workspaceId, String characterId) {
        String memberId = WorkspaceMember.createId(workspaceId, characterId);
        getMemberStore().delete(memberId);

        // 同步删除 asset_association 记录
        assetAssociationService.removeAssociation(
                AssociationType.CHARACTER_WORKSPACE,
                ResourceType.CHARACTER, characterId,
                ResourceType.WORKSPACE, workspaceId);

        log.info("[WorkspaceMemberManagementService] Removed member {} from workspace {}",
                characterId, workspaceId);
    }

    /**
     * 获取工作空间所有成员
     *
     * @param workspaceId 工作空间 ID
     * @return 成员列表
     */
    public List<WorkspaceMember> listMembers(String workspaceId) {
        return getMemberStore().findByWorkspaceId(workspaceId);
    }

    /**
     * 获取工作空间特定成员
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @return Optional 成员
     */
    public Optional<WorkspaceMember> getMember(String workspaceId, String characterId) {
        return getMemberStore().findByWorkspaceIdAndCharacterId(workspaceId, characterId);
    }

    /**
     * 更新成员角色
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param role 新角色
     */
    public void updateMemberRole(String workspaceId, String characterId, String role) {
        WorkspaceMember member = getMemberStore().findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setRole(role);
        member.setLastActiveAt(LocalDateTime.now());
        getMemberStore().update(member);
        log.info("[WorkspaceMemberManagementService] Updated member {} role to {} in workspace {}",
                characterId, role, workspaceId);
    }


    /**
     * 获取 Character 所属的所有工作空间
     *
     * @param characterId Character ID
     * @return 工作空间列表
     */
    public List<Workspace> getWorkspacesForCharacter(String characterId) {
        List<WorkspaceMember> memberships = getMemberStore().findByCharacterId(characterId);
        return memberships.stream()
                .map(m -> workspaceRegistry.get(m.getWorkspaceId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * 更新成员权重
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param weight 新权重
     */
    public void updateMemberWeight(String workspaceId, String characterId, double weight) {
        WorkspaceMember member = getMemberStore().findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setWeight(weight);
        member.setLastActiveAt(LocalDateTime.now());
        getMemberStore().update(member);
    }

    /**
     * 更新成员优先级
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param priority 新优先级
     */
    public void updateMemberPriority(String workspaceId, String characterId, int priority) {
        WorkspaceMember member = getMemberStore().findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setPriority(priority);
        member.setLastActiveAt(LocalDateTime.now());
        getMemberStore().update(member);
    }

    /**
     * 更新成员声誉积分
     *
     * @param workspaceId 工作空间 ID
     * @param characterId Character ID
     * @param reputationChange 声誉积分变化（正负值）
     */
    public void updateMemberReputation(String workspaceId, String characterId, int reputationChange) {
        WorkspaceMember member = getMemberStore().findByWorkspaceIdAndCharacterId(workspaceId, characterId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        int newReputation = member.getReputation() + reputationChange;
        member.setReputation(Math.max(0, newReputation)); // 不允许负值
        member.setLastActiveAt(LocalDateTime.now());
        getMemberStore().update(member);

        log.info("[WorkspaceMemberManagementService] Updated member {} reputation by {} in workspace {}, new value: {}",
                characterId, reputationChange, workspaceId, member.getReputation());
    }

    /**
     * 获取成员数量
     *
     * @param workspaceId 工作空间 ID
     * @return 成员数量
     */
    public int getMemberCount(String workspaceId) {
        return getMemberStore().countByWorkspaceId(workspaceId);
    }

    /**
     * 添加岗位
     *
     * @param workspaceId 工作空间 ID
     * @param roleName 角色名称
     * @param rolePackage 角色包/分类
     * @param purpose 岗位目的/职责
     * @param scope Scope 标识，定义工作域
     * @return 添加的岗位
     */
    public WorkspaceMember addPosition(String workspaceId, String roleName,
            String rolePackage, String purpose, String scope) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 创建岗位（无 assigned character）
        String id = workspaceId + "_" + roleName;
        WorkspaceMember position = WorkspaceMember.builder()
                .id(id)
                .workspaceId(workspaceId)
                .roleName(roleName)
                .rolePackage(rolePackage)
                .purpose(purpose)
                .scope(scope)
                .handlerType(HandlerType.BUILTIN_CHARACTER)
                .handlerId(null)  // 稍后分配
                .enabled(true)
                .layer(WorkspaceMember.Layer.NORMAL)
                .weight(1.0)
                .priority(0)
                .reputation(0)
                .joinAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        getMemberStore().save(position);

        log.info("[WorkspaceMemberService] Added position {} to workspace {}",
                roleName, workspaceId);

        return position;
    }

    /**
     * 更新岗位 Handler（Built-in Character 或 External Hook）
     *
     * @param workspaceId 工作空间 ID
     * @param positionId 岗位 ID
     * @param handlerType 处理方式
     * @param handlerId Handler ID（Character ID 或 Hook ID）
     */
    public void updatePositionHandler(String workspaceId, String positionId,
            HandlerType handlerType, String handlerId) {
        String memberId = workspaceId + "_" + positionId;
        WorkspaceMember position = getMemberStore().findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));

        position.setHandlerType(handlerType);
        position.setHandlerId(handlerId);
        position.setLastActiveAt(LocalDateTime.now());
        getMemberStore().update(position);

        // 如果使用 Built-in Character，同步创建 asset_association
        if (handlerType == HandlerType.BUILTIN_CHARACTER && handlerId != null) {
            assetAssociationService.createAssociation(
                    AssociationType.CHARACTER_WORKSPACE,
                    ResourceType.CHARACTER, handlerId,
                    ResourceType.WORKSPACE, workspaceId);
        }

        log.info("[WorkspaceMemberService] Updated position {} handler to {} ({}) in workspace {}",
                positionId, handlerType, handlerId, workspaceId);
    }

    /**
     * 列出工作空间所有岗位
     *
     * @param workspaceId 工作空间 ID
     * @return 岗位列表
     */
    public List<WorkspaceMember> listPositions(String workspaceId) {
        return getMemberStore().findByWorkspaceId(workspaceId);
    }

    /**
     * 获取特定岗位
     *
     * @param workspaceId 工作空间 ID
     * @param positionId 岗位 ID
     * @return Optional 岗位
     */
    public Optional<WorkspaceMember> getPosition(String workspaceId, String positionId) {
        String memberId = workspaceId + "_" + positionId;
        return getMemberStore().findById(memberId);
    }

    /**
     * 删除岗位
     *
     * @param workspaceId 工作空间 ID
     * @param positionId 岗位 ID
     */
    public void removePosition(String workspaceId, String positionId) {
        String memberId = workspaceId + "_" + positionId;
        getMemberStore().delete(memberId);

        log.info("[WorkspaceMemberService] Removed position {} from workspace {}",
                positionId, workspaceId);
    }
}

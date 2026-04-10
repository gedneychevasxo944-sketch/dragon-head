package org.dragon.workspace.service.teampositions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.TeamPositionResponse;
import org.dragon.store.StoreFactory;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.member.TeamPosition;
import org.dragon.workspace.member.TeamPositionStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * WorkspaceTeamPositionsManagementService 工作空间团队岗位管理服务
 *
 * @author qieqie
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceTeamPositionsManagementService {

    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;

    /**
     * 获取工作空间所有岗位
     *
     * @param workspaceId 工作空间 ID
     * @return 岗位列表
     */
    public List<TeamPosition> listPositions(String workspaceId) {
        return getPositionStore().findByWorkspaceId(workspaceId);
    }

    /**
     * 获取工作空间特定岗位
     *
     * @param workspaceId 工作空间 ID
     * @param positionId  岗位 ID
     * @return Optional 岗位
     */
    public Optional<TeamPosition> getPosition(String workspaceId, String positionId) {
        return getPositionStore().findById(positionId);
    }

    /**
     * 添加岗位到工作空间
     *
     * @param workspaceId  工作空间 ID
     * @param roleName    角色名称
     * @param rolePackage 角色包/分类
     * @param purpose     岗位目的
     * @param scope       岗位范围
     * @return 添加的岗位
     */
    public TeamPosition addPosition(String workspaceId, String roleName,
                                   String rolePackage, String purpose, String scope) {
        // 验证工作空间存在
        workspaceRegistry.get(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

        // 检查岗位是否已存在
        Optional<TeamPosition> existing = getPositionStore().findByWorkspaceIdAndRoleName(workspaceId, roleName);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Position already exists in workspace: " + roleName);
        }

        // 创建岗位
        TeamPosition position = TeamPosition.builder()
                .id(TeamPosition.createId(workspaceId, roleName))
                .workspaceId(workspaceId)
                .roleName(roleName)
                .rolePackage(rolePackage)
                .purpose(purpose)
                .scope(scope)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        getPositionStore().save(position);
        log.info("[WorkspaceTeamPositionsManagementService] Added position {} to workspace {}",
                roleName, workspaceId);

        return position;
    }

    /**
     * 更新岗位
     *
     * @param workspaceId         工作空间 ID
     * @param positionId          岗位 ID
     * @param assignedCharacterId 分配的 Character ID（可为 null）
     * @param assignedBuiltinType 分配的 Built-in 类型（可为 null，格式: "builtin:{type}"）
     * @param enabled             是否启用
     * @return 更新后的岗位
     */
    public TeamPosition updatePosition(String workspaceId, String positionId,
                                       String assignedCharacterId, String assignedBuiltinType, Boolean enabled) {
        TeamPosition position = getPositionStore().findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));

        // 验证归属
        if (!workspaceId.equals(position.getWorkspaceId())) {
            throw new IllegalArgumentException("Position does not belong to workspace: " + workspaceId);
        }

        // 更新字段 - assignedCharacterId 和 assignedBuiltinType 互斥
        if (assignedBuiltinType != null && !assignedBuiltinType.isBlank()) {
            position.setAssignedBuiltinType(assignedBuiltinType);
            position.setAssignedCharacterId(null);  // 清除普通 Character 分配
        } else if (assignedCharacterId != null) {
            position.setAssignedCharacterId(assignedCharacterId);
            position.setAssignedBuiltinType(null);  // 清除 Built-in 分配
        }
        if (enabled != null) {
            position.setEnabled(enabled);
        }
        position.setUpdatedAt(LocalDateTime.now());

        getPositionStore().update(position);
        log.info("[WorkspaceTeamPositionsManagementService] Updated position {} in workspace {}",
                positionId, workspaceId);

        return position;
    }

    /**
     * 删除岗位
     *
     * @param workspaceId 工作空间 ID
     * @param positionId  岗位 ID
     */
    public void deletePosition(String workspaceId, String positionId) {
        TeamPosition position = getPositionStore().findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("Position not found: " + positionId));

        // 验证归属
        if (!workspaceId.equals(position.getWorkspaceId())) {
            throw new IllegalArgumentException("Position does not belong to workspace: " + workspaceId);
        }

        getPositionStore().delete(positionId);
        log.info("[WorkspaceTeamPositionsManagementService] Deleted position {} from workspace {}",
                positionId, workspaceId);
    }

    /**
     * 将岗位转换为响应DTO
     *
     * @param position 岗位
     * @return TeamPositionResponse
     */
    public TeamPositionResponse toResponse(TeamPosition position) {
        return TeamPositionResponse.from(position);
    }

    /**
     * 批量将岗位转换为响应DTO
     *
     * @param positions 岗位列表
     * @return TeamPositionResponse 列表
     */
    public List<TeamPositionResponse> toResponseList(List<TeamPosition> positions) {
        return positions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TeamPositionStore getPositionStore() {
        return storeFactory.get(TeamPositionStore.class);
    }
}

package org.dragon.workspace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.asset.dto.AssetAssociationDTO;
import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.permission.enums.ResourceType;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeploymentService 派驻服务
 *
 * <p>负责 Character 到 Workspace 的派驻管理，
 * 使用 asset 关联记录派驻关系。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentService {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final AssetAssociationService assetAssociationService;
    private final WorkspaceMemberManagementService memberManagementService;

    /**
     * 获取派驻记录列表（Character 已部署到的 Workspace 映射）。
     *
     * @param page        页码
     * @param pageSize    每页数量
     * @param characterId 按角色 ID 筛选（可选）
     * @param workspaceId 按 Workspace ID 筛选（可选）
     * @return 分页派驻记录
     */
    public List<Map<String, Object>> listDeployments(int page, int pageSize,
                                                     String characterId, String workspaceId) {
        // 从 asset 关联获取派驻记录
        List<AssetAssociationDTO> associations;
        if (workspaceId != null && !workspaceId.isBlank()) {
            associations = assetAssociationService.findByTarget(
                    AssociationType.CHARACTER_WORKSPACE,
                    ResourceType.WORKSPACE, workspaceId);
        } else {
            associations = assetAssociationService.findByType(AssociationType.CHARACTER_WORKSPACE);
        }

        // 过滤
        List<Map<String, Object>> deployments = associations.stream()
                .filter(a -> characterId == null || characterId.isBlank() || characterId.equals(a.getSourceId()))
                .map(this::toDeploymentRecord)
                .collect(Collectors.toList());

        return deployments;
    }

    /**
     * 派驻角色到 Workspace。
     *
     * @param characterId 角色 ID
     * @param workspaceId 目标 Workspace ID
     * @param role        职责
     * @param position    职位
     * @param level       级别
     * @return 派驻记录
     */
    public Map<String, Object> deployCharacter(String characterId, String workspaceId,
                                               String role, String position, Integer level) {
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // 创建 asset 关联
        assetAssociationService.createAssociation(
                AssociationType.CHARACTER_WORKSPACE,
                ResourceType.CHARACTER, characterId,
                ResourceType.WORKSPACE, workspaceId);

        // 同时加入 workspace 成员
        try {
            memberManagementService.addMember(workspaceId, characterId, role != null ? role : "member", WorkspaceMember.Layer.NORMAL);
        } catch (Exception e) {
            log.warn("[DeploymentService] Failed to add member to workspace {}: {}", workspaceId, e.getMessage());
        }

        return buildDeploymentRecord(character, workspaceId, role, position, level);
    }

    /**
     * 撤销派驻。
     *
     * @param deploymentId 格式为 characterId_workspaceId
     */
    public void undeployCharacter(String deploymentId) {
        String[] parts = deploymentId.split("_", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid deploymentId: " + deploymentId);
        }
        String charId = parts[0];
        String wsId = parts[1];

        // 删除 asset 关联
        assetAssociationService.removeAssociation(
                AssociationType.CHARACTER_WORKSPACE,
                ResourceType.CHARACTER, charId,
                ResourceType.WORKSPACE, wsId);

        // 同时移除 workspace 成员
        try {
            memberManagementService.removeMember(wsId, charId);
        } catch (Exception e) {
            log.warn("[DeploymentService] Failed to remove member from workspace {}: {}", wsId, e.getMessage());
        }

        log.info("[DeploymentService] Undeployed character {} from workspace {}", charId, wsId);
    }

    private Map<String, Object> toDeploymentRecord(org.dragon.asset.dto.AssetAssociationDTO a) {
        String charId = a.getSourceId();
        String wsId = a.getTargetId();

        Character character = characterRegistry.get(charId).orElse(null);
        String charName = character != null && character.getName() != null ? character.getName() : "";
        String charStatus = character != null && character.getStatus() != null
                ? character.getStatus().name().toLowerCase() : "idle";

        org.dragon.workspace.Workspace workspace = workspaceRegistry.get(wsId).orElse(null);
        String wsName = workspace != null && workspace.getName() != null ? workspace.getName() : "Workspace " + wsId;

        java.util.HashMap<String, Object> record = new java.util.HashMap<>();
        record.put("id", charId + "_" + wsId);
        record.put("characterId", charId);
        record.put("characterName", charName);
        record.put("workspaceId", wsId);
        record.put("workspaceName", wsName);
        record.put("role", "Member");
        record.put("position", "AI Agent");
        record.put("level", 3);
        record.put("status", charStatus);
        record.put("deployedAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
        record.put("lastActiveAt", character != null && character.getUpdatedAt() != null
                ? character.getUpdatedAt().toString() : "");
        record.put("hasOverrides", false);
        return record;
    }

    private Map<String, Object> buildDeploymentRecord(Character c, String wid, String role, String position, Integer level) {
        java.util.HashMap<String, Object> record = new java.util.HashMap<>();
        record.put("id", c.getId() + "_" + wid);
        record.put("characterId", c.getId());
        record.put("characterName", c.getName() != null ? c.getName() : "");
        record.put("workspaceId", wid);
        record.put("workspaceName", "Workspace " + wid);
        record.put("role", role != null ? role : "");
        record.put("position", position != null ? position : "");
        record.put("level", level != null ? level : 3);
        record.put("status", c.getStatus() != null ? c.getStatus().name().toLowerCase() : "idle");
        record.put("deployedAt", java.time.LocalDateTime.now().toString());
        record.put("lastActiveAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
        record.put("hasOverrides", false);
        return record;
    }
}
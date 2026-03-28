package org.dragon.user.permission;

import org.dragon.character.Character;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.store.StoreFactory;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.store.WorkspaceStore;
import org.dragon.workspace.member.WorkspaceMemberStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * PermissionService 权限服务
 * 负责资源可见性和权限校验
 */
@Service
public class PermissionService {

    private final WorkspaceStore workspaceStore;
    private final WorkspaceMemberStore workspaceMemberStore;

    public PermissionService(StoreFactory storeFactory) {
        this.workspaceStore = storeFactory.get(WorkspaceStore.class);
        this.workspaceMemberStore = storeFactory.get(WorkspaceMemberStore.class);
    }

    /**
     * 检查用户是否可以查看Workspace
     */
    public boolean canViewWorkspace(Long userId, String workspaceId) {
        Optional<Workspace> workspace = workspaceStore.findById(workspaceId);
        if (workspace.isEmpty()) {
            return false;
        }

        Workspace ws = workspace.get();
        String visibility = ws.getProperties() != null && ws.getProperties().containsKey("visibility")
                ? ws.getProperties().get("visibility").toString()
                : "PRIVATE";

        // PUBLIC workspace 所有登录用户可见
        if ("PUBLIC".equals(visibility)) {
            return true;
        }

        // PRIVATE workspace 仅成员可见
        return isWorkspaceMember(userId, workspaceId);
    }

    /**
     * 检查用户是否是Workspace成员
     */
    public boolean isWorkspaceMember(Long userId, String workspaceId) {
        // workspace的owner就是创建者，这里用userId的字符串形式
        // 注意：Workspace.owner 是 String 类型，而 userId 是 Long 类型
        // 需要根据实际设计判断是否匹配
        Optional<Workspace> workspace = workspaceStore.findById(workspaceId);
        if (workspace.isPresent() && String.valueOf(userId).equals(workspace.get().getOwner())) {
            return true;
        }

        // 检查成员表
        List<WorkspaceMember> members = workspaceMemberStore.findByWorkspaceId(workspaceId);
        return members.stream()
                .anyMatch(m -> String.valueOf(userId).equals(m.getCharacterId()));
    }

    /**
     * 检查用户是否可以查看Character
     * 规则：
     * - 属于Workspace的Character：仅Workspace成员可见
     * - 不属于任何Workspace的Character：仅创建者可见
     */
    public boolean canViewCharacter(Long userId, Character character) {
        // 如果Character属于某个Workspace
        List<String> workspaceIds = character.getWorkspaceIds();
        if (workspaceIds != null && !workspaceIds.isEmpty()) {
            // 检查是否是Workspace成员
            for (String wsId : workspaceIds) {
                if (isWorkspaceMember(userId, wsId)) {
                    return true;
                }
            }
            return false;
        }

        // 不属于任何Workspace，检查是否是创建者
        // 注意：Character的创建者信息需要从CharacterProfile中获取
        // 目前Character模型中没有直接的creatorId字段，需要后续扩展
        return false;
    }

    /**
     * 检查用户是否可以查看Skill
     * 规则：
     * - PUBLIC：所有登录用户可见
     * - PRIVATE：仅创建者可见
     */
    public boolean canViewSkill(Long userId, SkillEntity skill) {
        if (skill.getVisibility() == SkillVisibility.PUBLIC) {
            return true;
        }
        // PRIVATE 仅创建者可见
        return skill.getCreatorId() != null && skill.getCreatorId().equals(userId);
    }

    /**
     * 检查用户是否是Workspace所有者
     */
    public boolean isWorkspaceOwner(Long userId, String workspaceId) {
        Optional<Workspace> workspace = workspaceStore.findById(workspaceId);
        return workspace.isPresent() && String.valueOf(userId).equals(workspace.get().getOwner());
    }

    /**
     * 获取用户在Workspace中的权限级别
     */
    public PermissionEnums.WorkspacePermission getWorkspacePermission(Long userId, String workspaceId) {
        if (isWorkspaceOwner(userId, workspaceId)) {
            return PermissionEnums.WorkspacePermission.OWNER;
        }

        List<WorkspaceMember> members = workspaceMemberStore.findByWorkspaceId(workspaceId);
        return members.stream()
                .filter(m -> String.valueOf(userId).equals(m.getCharacterId()))
                .findFirst()
                .map(m -> {
                    // 根据role映射到权限
                    String role = m.getRole();
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        return PermissionEnums.WorkspacePermission.ADMIN;
                    }
                    return PermissionEnums.WorkspacePermission.VIEW;
                })
                .orElse(PermissionEnums.WorkspacePermission.VIEW);
    }

    /**
     * 获取用户可见的Workspace列表
     */
    public List<Workspace> getVisibleWorkspaces(Long userId) {
        // 获取所有PUBLIC workspace + 用户所在的PRIVATE workspace
        List<Workspace> allWorkspaces = workspaceStore.findAll();
        return allWorkspaces.stream()
                .filter(ws -> canViewWorkspace(userId, ws.getId()))
                .toList();
    }

    /**
     * 验证API访问权限（用于Spring Security @PreAuthorize）
     */
    public boolean checkApiAccess() {
        // 这个方法在@PreAuthorize中调用
        // 如果能执行到这里，说明JWT认证已经通过，用户已登录
        return true;
    }
}

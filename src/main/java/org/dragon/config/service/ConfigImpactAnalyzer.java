package org.dragon.config.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.enums.ConfigScopeType;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.store.StoreFactory;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.member.WorkspaceMemberStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ConfigImpactAnalyzer 配置影响分析服务
 *
 * <p>分析配置变更会影响哪些实体
 *
 * <p>分析维度：
 * <ul>
 *   <li>Workspace 配置 → 影响该 workspace 下的所有 characters、members、skills</li>
 *   <li>Character 配置 → 影响使用该 character 的所有 workspaces</li>
 *   <li>Memory 配置 → 通过 owner_type/owner_id 追踪影响范围（Character 或 Workspace）</li>
 *   <li>Skill 配置 → 影响绑定该 skill 的所有 workspace-skill bindings 和使用的 tools</li>
 *   <li>Tool 配置 → 影响使用该 tool 的所有 skills</li>
 * </ul>
 */
@Slf4j
@Service
public class ConfigImpactAnalyzer {

    private final WorkspaceMemberStore workspaceMemberStore;
    private final SkillBindingStore skillBindingStore;

    @Autowired
    public ConfigImpactAnalyzer(WorkspaceMemberStore workspaceMemberStore, SkillBindingStore skillBindingStore) {
        this.workspaceMemberStore = workspaceMemberStore;
        this.skillBindingStore = skillBindingStore;
    }

    /**
     * 分析配置变更的影响范围
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param configKey 配置键
     * @return 影响分析结果
     */
    public ImpactAnalysis analyzeImpact(ConfigScopeType scopeType, String scopeId, String configKey) {
        List<ImpactItem> impacts = new ArrayList<>();

        switch (scopeType) {
            case WORKSPACE:
                impacts.addAll(analyzeWorkspaceImpact(scopeId));
                break;
            case CHARACTER:
                impacts.addAll(analyzeCharacterImpact(scopeId));
                break;
            case MEMORY:
                impacts.addAll(analyzeMemoryImpact(scopeId));
                break;
            case SKILL:
                impacts.addAll(analyzeSkillImpact(scopeId));
                break;
            case TOOL:
                impacts.addAll(analyzeToolImpact(scopeId));
                break;
            case OBSERVER:
                impacts.addAll(analyzeObserverImpact(scopeId));
                break;
            case MEMBER:
                impacts.addAll(analyzeMemberImpact(scopeId));
                break;
            case GLOBAL:
            case STUDIO:
                // 全局和 Studio 配置影响广泛但难以精确追踪
                impacts.add(ImpactItem.builder()
                        .resourceType("GLOBAL")
                        .resourceId("-")
                        .impactType(ImpactType.BROAD)
                        .description("全局/Studio 配置影响所有子实体")
                        .build());
                break;
            case WORKSPACE_REF_OVERRIDE:
                // workspace_ref_override 影响被引用的特定资产
                impacts.add(ImpactItem.builder()
                        .resourceType("WORKSPACE_REF_OVERRIDE")
                        .resourceId(scopeId)
                        .impactType(ImpactType.DIRECT)
                        .description("Workspace 引用覆盖配置，只影响特定资产")
                        .build());
                break;
        }

        return ImpactAnalysis.builder()
                .scopeType(scopeType)
                .scopeId(scopeId)
                .configKey(configKey)
                .impacts(impacts)
                .totalAffected(impacts.size())
                .build();
    }

    /**
     * 分析 Workspace 配置的影响
     * <p>
     * Workspace 配置会影响：
     * - 该 workspace 下的所有 characters（通过 member 关系）
     * - 该 workspace 下的所有 members
     * - 该 workspace 下的所有 skill bindings
     */
    private List<ImpactItem> analyzeWorkspaceImpact(String workspaceId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // 获取 workspace 下的所有成员
        List<WorkspaceMember> members = workspaceMemberStore.findByWorkspaceId(workspaceId);
        for (WorkspaceMember member : members) {
            impacts.add(ImpactItem.builder()
                    .resourceType(ResourceType.CHARACTER.name())
                    .resourceId(member.getCharacterId())
                    .impactType(ImpactType.INHERITED)
                    .description("Character 继承 Workspace 配置: " + workspaceId)
                    .build());
        }

        // 获取 workspace 下的所有公共 skill bindings
        List<SkillBindingDO> skillBindings = skillBindingStore.findByWorkspaceId(workspaceId);
        for (SkillBindingDO binding : skillBindings) {
            impacts.add(ImpactItem.builder()
                    .resourceType(ResourceType.SKILL.name())
                    .resourceId(binding.getSkillId())
                    .impactType(ImpactType.INHERITED)
                    .description("Skill 绑定在 Workspace 下: " + workspaceId)
                    .build());
        }

        log.info("[ConfigImpactAnalyzer] Workspace {} config impacts {} entities", workspaceId, impacts.size());
        return impacts;
    }

    /**
     * 分析 Character 配置的影响
     * <p>
     * Character 配置会影响：
     * - 该 character 加入的所有 workspaces
     */
    private List<ImpactItem> analyzeCharacterImpact(String characterId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // 获取 character 加入的所有 workspaces
        List<WorkspaceMember> memberships = workspaceMemberStore.findByCharacterId(characterId);
        for (WorkspaceMember member : memberships) {
            impacts.add(ImpactItem.builder()
                    .resourceType(ResourceType.WORKSPACE.name())
                    .resourceId(member.getWorkspaceId())
                    .impactType(ImpactType.DIRECT)
                    .description("Workspace 使用 Character: " + characterId)
                    .build());
        }

        log.info("[ConfigImpactAnalyzer] Character {} config impacts {} workspaces", characterId, impacts.size());
        return impacts;
    }

    /**
     * 分析 Memory 配置的影响
     * <p>
     * Memory 归属关系通过 owner_type 和 owner_id 追踪：
     * - owner_type = CHARACTER: Memory 属于某个 Character
     * - owner_type = WORKSPACE: Memory 属于某个 Workspace
     * <p>
     * 注意：实际实现需要查询 ConfigEntity 获取 owner 信息，
     * 这里简化处理，假设 Memory 配置变更影响其归属者
     */
    private List<ImpactItem> analyzeMemoryImpact(String memoryId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // TODO: 需要从 ConfigEntity 或 Memory 表查询 owner 信息
        // 暂时标记为需要进一步查询
        impacts.add(ImpactItem.builder()
                .resourceType(ResourceType.MEMORY.name())
                .resourceId(memoryId)
                .impactType(ImpactType.OWNER_DEPENDENT)
                .description("Memory 配置影响其归属者（需查询 owner 信息）")
                .build());

        log.info("[ConfigImpactAnalyzer] Memory {} config impact analysis pending owner lookup", memoryId);
        return impacts;
    }

    /**
     * 分析 Skill 配置的影响
     * <p>
     * Skill 配置会影响：
     * - 绑定该 skill 的所有 workspace-skill bindings
     */
    private List<ImpactItem> analyzeSkillImpact(String skillId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // 查询所有使用该 skill 的 workspace bindings
        // 注意：SkillBindingStore 没有直接按 skillId 查询的方法，需要遍历或添加
        // 暂时通过查找 workspace bindings 然后过滤
        // TODO: 添加 SkillBindingStore.findBySkillId() 方法

        impacts.add(ImpactItem.builder()
                .resourceType(ResourceType.SKILL.name())
                .resourceId(skillId)
                .impactType(ImpactType.DIRECT)
                .description("Skill 配置影响所有使用该 skill 的 bindings（需完善查询）")
                .build());

        log.info("[ConfigImpactAnalyzer] Skill {} config impacts {} entities", skillId, impacts.size());
        return impacts;
    }

    /**
     * 分析 Tool 配置的影响
     * <p>
     * Tool 配置会影响：
     * - 使用该 tool 的所有 skills
     * <p>
     * 注意：这需要追踪 tool_skill_usage 关系
     */
    private List<ImpactItem> analyzeToolImpact(String toolId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // TODO: 需要查询 tool_skill_usage 或类似关系表
        // 暂时标记为需要进一步查询
        impacts.add(ImpactItem.builder()
                .resourceType(ResourceType.TOOL.name())
                .resourceId(toolId)
                .impactType(ImpactType.REFERENCED)
                .description("Tool 配置影响使用该 tool 的所有 skills（需查询 skill_tool_usage）")
                .build());

        log.info("[ConfigImpactAnalyzer] Tool {} config impact analysis pending skill usage lookup", toolId);
        return impacts;
    }

    /**
     * 分析 Observer 配置的影响
     * <p>
     * Observer 配置会影响：
     * - 该 observer 加入的所有 workspaces
     */
    private List<ImpactItem> analyzeObserverImpact(String observerId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // TODO: 需要查询 observer_workspace 关系
        // 暂时标记为需要进一步查询
        impacts.add(ImpactItem.builder()
                .resourceType(ResourceType.OBSERVER.name())
                .resourceId(observerId)
                .impactType(ImpactType.REFERENCED)
                .description("Observer 配置影响使用该 observer 的 workspaces（需查询关系）")
                .build());

        log.info("[ConfigImpactAnalyzer] Observer {} config impact analysis pending workspace lookup", observerId);
        return impacts;
    }

    /**
     * 分析 Member 配置的影响
     * <p>
     * Member 配置存储在 WORKSPACE 下（targetType = MEMBER）
     * scopeId 对于 MEMBER 是 workspaceId
     * 影响范围是该 workspace 下的特定成员
     */
    private List<ImpactItem> analyzeMemberImpact(String workspaceId) {
        List<ImpactItem> impacts = new ArrayList<>();

        // 获取 workspace 下的所有成员
        List<WorkspaceMember> members = workspaceMemberStore.findByWorkspaceId(workspaceId);
        for (WorkspaceMember member : members) {
            impacts.add(ImpactItem.builder()
                    .resourceType(ResourceType.CHARACTER.name())
                    .resourceId(member.getCharacterId())
                    .impactType(ImpactType.DIRECT)
                    .description("Member 配置影响 Character: " + member.getCharacterId())
                    .build());
        }

        log.info("[ConfigImpactAnalyzer] Workspace {} member configs impact {} characters", workspaceId, impacts.size());
        return impacts;
    }

    /**
     * 影响分析结果
     */
    @Data
    @Builder
    public static class ImpactAnalysis {
        /** 作用域类型 */
        private ConfigScopeType scopeType;
        /** 作用域 ID */
        private String scopeId;
        /** 配置键 */
        private String configKey;
        /** 影响项列表 */
        private List<ImpactItem> impacts;
        /** 受影响实体总数 */
        private int totalAffected;
    }

    /**
     * 影响项
     */
    @Data
    @Builder
    public static class ImpactItem {
        /** 资源类型 */
        private String resourceType;
        /** 资源 ID */
        private String resourceId;
        /** 影响类型 */
        private ImpactType impactType;
        /** 影响描述 */
        private String description;
    }

    /**
     * 影响类型枚举
     */
    public enum ImpactType {
        /** 直接影响：该配置直接作用于该实体 */
        DIRECT,
        /** 继承影响：实体继承父级配置 */
        INHERITED,
        /** 引用影响：通过引用关系传递影响 */
        REFERENCED,
        /** 归属依赖：影响取决于归属关系 */
        OWNER_DEPENDENT,
        /** 广泛影响：影响难以精确追踪 */
        BROAD
    }
}
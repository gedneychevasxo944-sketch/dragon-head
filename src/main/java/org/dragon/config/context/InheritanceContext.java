package org.dragon.config.context;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigScopeType;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置继承上下文
 *
 * <p>用于传递配置查询的层级链上下文，调用方传入从全局到具体的层级链，
 * Config 系统内部根据此上下文计算配置的生效值。
 *
 * <p>继承链顺序（从高到低）：
 * GLOBAL → STUDIO → WORKSPACE → CHARACTER → TOOL → SKILL → ...
 *                                            ↑ 此处为具体配置
 *
 * <p>使用示例：
 * <pre>
 * // 查询 Character 使用 Tool 时的配置
 * InheritanceContext context = InheritanceContext.builder()
 *     .scopes(new ArrayList<>(List.of(
 *         ContextScope.of(ConfigScopeType.GLOBAL, "-"),
 *         ContextScope.of(ConfigScopeType.STUDIO, studioId),
 *         ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId),
 *         ContextScope.of(ConfigScopeType.CHARACTER, characterId),
 *         ContextScope.of(ConfigScopeType.TOOL, toolId)
 *     )))
 *     .build();
 *
 * // 查询 Workspace 自己的配置
 * InheritanceContext context = InheritanceContext.forWorkspace(studioId, workspaceId);
 * </pre>
 */
@Data
@Builder
public class InheritanceContext {

    /**
     * 作用域链（从全局到具体）
     */
    @Builder.Default
    private List<ContextScope> scopes = new ArrayList<>();

    /**
     * 添加作用域
     */
    public void addScope(ConfigScopeType scopeType, String scopeId) {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        scopes.add(ContextScope.builder()
                .scopeType(scopeType)
                .scopeId(scopeId)
                .build());
    }

    /**
     * 添加作用域（带目标 ID，用于 MEMBER 等场景）
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param targetId 目标 ID（如 MEMBER 场景下的 memberId）
     */
    public void addScope(ConfigScopeType scopeType, String scopeId, String targetId) {
        if (scopes == null) {
            scopes = new ArrayList<>();
        }
        scopes.add(ContextScope.builder()
                .scopeType(scopeType)
                .scopeId(scopeId)
                .targetId(targetId)
                .build());
    }

    /**
     * 获取继承链（从具体到全局）
     */
    public List<ContextScope> getInheritanceChain() {
        List<ContextScope> chain = new ArrayList<>(scopes);
        // 反转：从具体到全局
        java.util.Collections.reverse(chain);
        return chain;
    }

    /**
     * 获取最高优先级层级（链的头部，即最具体的那一层）
     */
    public ContextScope getMostSpecificScope() {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return scopes.get(scopes.size() - 1);
    }

    /**
     * 获取最低优先级层级（链的尾部，即全局层）
     */
    public ContextScope getGlobalScope() {
        if (scopes == null || scopes.isEmpty()) {
            return null;
        }
        return scopes.get(0);
    }

    /**
     * 作用域中的单个层级
     */
    @Data
    @Builder
    public static class ContextScope {
        /** 作用域类型 */
        private ConfigScopeType scopeType;
        /** 作用域 ID */
        private String scopeId;
        /** 目标 ID（如 MEMBER 场景下的 memberId，WORKSPACE_REF_OVERRIDE 场景下的被引用资产 ID） */
        private String targetId;

        /**
         * 便捷工厂方法
         */
        public static ContextScope of(ConfigScopeType scopeType, String scopeId) {
            return ContextScope.builder()
                    .scopeType(scopeType)
                    .scopeId(scopeId)
                    .build();
        }

        public static ContextScope of(ConfigScopeType scopeType, String scopeId, String targetId) {
            return ContextScope.builder()
                    .scopeType(scopeType)
                    .scopeId(scopeId)
                    .targetId(targetId)
                    .build();
        }
    }

    // ==================== 便捷工厂方法 ====================

    /**
     * 构建 Workspace 自己的配置上下文
     */
    public static InheritanceContext forWorkspace(String studioId, String workspaceId) {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        scopeList.add(ContextScope.of(ConfigScopeType.STUDIO, studioId));
        scopeList.add(ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId));
        return InheritanceContext.builder().scopes(scopeList).build();
    }

    /**
     * 构建 Character 自己的配置上下文
     *
     * @param studioId Studio ID（可选，为 null 时跳过 STUDIO 层）
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     */
    public static InheritanceContext forCharacter(String studioId, String workspaceId, String characterId) {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        if (studioId != null) {
            scopeList.add(ContextScope.of(ConfigScopeType.STUDIO, studioId));
        }
        scopeList.add(ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId));
        scopeList.add(ContextScope.of(ConfigScopeType.CHARACTER, characterId));
        return InheritanceContext.builder().scopes(scopeList).build();
    }

    /**
     * 构建 Character 调用 Tool 时的配置上下文
     *
     * @param studioId Studio ID（可选，为 null 时跳过 STUDIO 层）
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     * @param toolId Tool ID
     */
    public static InheritanceContext forCharacterTool(String studioId, String workspaceId,
                                                       String characterId, String toolId) {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        if (studioId != null) {
            scopeList.add(ContextScope.of(ConfigScopeType.STUDIO, studioId));
        }
        scopeList.add(ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId));
        scopeList.add(ContextScope.of(ConfigScopeType.CHARACTER, characterId));
        scopeList.add(ContextScope.of(ConfigScopeType.TOOL, toolId));
        return InheritanceContext.builder().scopes(scopeList).build();
    }

    /**
     * 构建 Skill 自己的配置上下文
     *
     * @param studioId Studio ID（可选，为 null 时跳过 STUDIO 层）
     * @param workspaceId Workspace ID
     * @param skillId Skill ID
     */
    public static InheritanceContext forSkill(String studioId, String workspaceId, String skillId) {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        if (studioId != null) {
            scopeList.add(ContextScope.of(ConfigScopeType.STUDIO, studioId));
        }
        scopeList.add(ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId));
        scopeList.add(ContextScope.of(ConfigScopeType.SKILL, skillId));
        return InheritanceContext.builder().scopes(scopeList).build();
    }

    /**
     * 构建全局配置上下文
     */
    public static InheritanceContext forGlobal() {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        return InheritanceContext.builder().scopes(scopeList).build();
    }

    /**
     * 构建 Observer 自己的配置上下文
     *
     * @param workspaceId Workspace ID（可选，为 null 时跳过 WORKSPACE 层）
     * @param observerId Observer ID
     */
    public static InheritanceContext forObserver(String workspaceId, String observerId) {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        if (workspaceId != null) {
            scopeList.add(ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId));
        }
        scopeList.add(ContextScope.of(ConfigScopeType.OBSERVER, observerId));
        return InheritanceContext.builder().scopes(scopeList).build();
    }

    /**
     * 构建 Memory 配置上下文
     *
     * @param workspaceId Workspace ID（可选，为 null 时跳过 WORKSPACE 层）
     * @param memoryId Memory ID
     */
    public static InheritanceContext forMemory(String workspaceId, String memoryId) {
        List<ContextScope> scopeList = new ArrayList<>();
        scopeList.add(ContextScope.of(ConfigScopeType.GLOBAL, "-"));
        if (workspaceId != null) {
            scopeList.add(ContextScope.of(ConfigScopeType.WORKSPACE, workspaceId));
        }
        scopeList.add(ContextScope.of(ConfigScopeType.MEMORY, memoryId));
        return InheritanceContext.builder().scopes(scopeList).build();
    }
}
package org.dragon.config.context;

import lombok.Builder;
import lombok.Data;
import org.dragon.config.enums.ConfigLevel;

/**
 * 配置继承上下文
 *
 * <p>用于传递配置查询的层级上下文，包含粒度和各层级 ID。
 *
 * <p>使用示例：
 * <pre>
 * // GLOBAL -> WORKSPACE -> CHARACTER -> TOOL 粒度
 * InheritanceContext context = InheritanceContext.builder()
 *     .level(ConfigLevel.GLOBAL_WS_CHAR_TOOL)
 *     .workspaceId("ws-001")
 *     .characterId("char-001")
 *     .toolId("tool-001")
 *     .build();
 *
 * // GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER 粒度
 * InheritanceContext studioContext = InheritanceContext.builder()
 *     .level(ConfigLevel.STUDIO_WS_CHAR)
 *     .workspaceId("ws-001")
 *     .characterId("char-001")
 *     .build();
 * </pre>
 */
@Data
@Builder
public class InheritanceContext {

    /**
     * 配置粒度
     */
    private ConfigLevel level;

    /**
     * WORKSPACE ID
     */
    private String workspaceId;

    /**
     * CHARACTER ID
     */
    private String characterId;

    /**
     * TOOL ID
     */
    private String toolId;

    /**
     * SKILL ID
     */
    private String skillId;

    /**
     * MEMORY ID
     */
    private String memoryId;

    // ==================== 便捷工厂方法 ====================

    /**
     * GLOBAL -> WORKSPACE
     */
    public static InheritanceContext forGlobalWorkspace(String workspaceId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WORKSPACE)
                .workspaceId(workspaceId)
                .build();
    }

    /**
     * GLOBAL -> CHARACTER
     */
    public static InheritanceContext forGlobalCharacter(String characterId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_CHARACTER)
                .characterId(characterId)
                .build();
    }

    /**
     * GLOBAL -> SKILL
     */
    public static InheritanceContext forGlobalSkill(String skillId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_SKILL)
                .skillId(skillId)
                .build();
    }

    /**
     * GLOBAL -> TOOL
     */
    public static InheritanceContext forGlobalTool(String toolId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_TOOL)
                .toolId(toolId)
                .build();
    }

    /**
     * GLOBAL -> MEMORY
     */
    public static InheritanceContext forGlobalMemory(String memoryId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_MEMORY)
                .memoryId(memoryId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> CHARACTER
     */
    public static InheritanceContext forGlobalWsChar(String workspaceId, String characterId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_CHAR)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> SKILL
     */
    public static InheritanceContext forGlobalWsSkill(String workspaceId, String skillId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_SKILL)
                .workspaceId(workspaceId)
                .skillId(skillId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> MEMORY
     */
    public static InheritanceContext forGlobalWsMemory(String workspaceId, String memoryId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_MEMORY)
                .workspaceId(workspaceId)
                .memoryId(memoryId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> TOOL
     */
    public static InheritanceContext forGlobalWsTool(String workspaceId, String toolId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_TOOL)
                .workspaceId(workspaceId)
                .toolId(toolId)
                .build();
    }

    /**
     * GLOBAL -> CHARACTER -> TOOL
     */
    public static InheritanceContext forGlobalCharTool(String characterId, String toolId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_CHAR_TOOL)
                .characterId(characterId)
                .toolId(toolId)
                .build();
    }

    /**
     * GLOBAL -> CHARACTER -> SKILL
     */
    public static InheritanceContext forGlobalCharSkill(String characterId, String skillId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_CHAR_SKILL)
                .characterId(characterId)
                .skillId(skillId)
                .build();
    }

    /**
     * GLOBAL -> CHARACTER -> MEMORY
     */
    public static InheritanceContext forGlobalCharMemory(String characterId, String memoryId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_CHAR_MEMORY)
                .characterId(characterId)
                .memoryId(memoryId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> CHARACTER -> TOOL
     */
    public static InheritanceContext forGlobalWsCharTool(String workspaceId, String characterId, String toolId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_CHAR_TOOL)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .toolId(toolId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> CHARACTER -> SKILL
     */
    public static InheritanceContext forGlobalWsCharSkill(String workspaceId, String characterId, String skillId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_CHAR_SKILL)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .skillId(skillId)
                .build();
    }

    /**
     * GLOBAL -> WORKSPACE -> CHARACTER -> MEMORY
     */
    public static InheritanceContext forGlobalWsCharMemory(String workspaceId, String characterId, String memoryId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WS_CHAR_MEMORY)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .memoryId(memoryId)
                .build();
    }

    // ==================== STUDIO 粒度工厂方法 ====================

    /**
     * GLOBAL -> STUDIO -> WORKSPACE
     */
    public static InheritanceContext forStudioWorkspace(String workspaceId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.STUDIO_WORKSPACE)
                .workspaceId(workspaceId)
                .build();
    }

    /**
     * GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER
     */
    public static InheritanceContext forStudioWsChar(String workspaceId, String characterId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.STUDIO_WS_CHAR)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .build();
    }

    /**
     * GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER -> TOOL
     */
    public static InheritanceContext forStudioWsCharTool(String workspaceId, String characterId, String toolId) {
        return InheritanceContext.builder()
                .level(ConfigLevel.STUDIO_WS_CHAR_TOOL)
                .workspaceId(workspaceId)
                .characterId(characterId)
                .toolId(toolId)
                .build();
    }

    /**
     * 全局配置上下文（兼容旧 API）
     */
    public static InheritanceContext forGlobal() {
        return InheritanceContext.builder()
                .level(ConfigLevel.GLOBAL_WORKSPACE)
                .build();
    }
}
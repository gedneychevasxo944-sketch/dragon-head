package org.dragon.config.store;

import org.dragon.config.enums.ConfigScopeType;

import java.util.Objects;

/**
 * 配置键
 *
 * <p>使用语义明确的静态工厂方法构造，config_key 格式：
 * <pre>{scopeType}:{scopeId}:{targetType}:{targetId}:{configKey}</pre>
 *
 * <p>targetType 说明：
 * <ul>
 *   <li>self：自有配置（配置本身属于自己的）</li>
 *   <li>CHARACTER/SKILL/OBSERVER/TOOL/MEMBER：引用配置（Workspace 引用其他资产）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * // 全局配置
 * ConfigKey.global("jwt.secret")
 *
 * // Studio 用户级配置
 * ConfigKey.studio("studio_123", "theme")
 *
 * // Workspace 自有配置
 * ConfigKey.workspace("ws_456", "maxSteps")
 *
 * // Workspace 引用 Character 的覆盖配置
 * ConfigKey.workspaceRefOverride("ws_456", "CHARACTER", "char_123", "maxSteps")
 *
 * // Character 配置
 * ConfigKey.character("char_789", "systemPrompt")
 *
 * // Memory 配置（归属 Character）
 * ConfigKey.memory("mem_001", "CHARACTER", "char_789", "maxMemorySize")
 *
 * // Memory 配置（归属 Workspace）
 * ConfigKey.memory("mem_002", "WORKSPACE", "ws_456", "maxMemorySize")
 *
 * // Observer 配置
 * ConfigKey.observer("obs_001", "evaluationMode")
 *
 * // Skill 配置
 * ConfigKey.skill("skill_123", "visibility")
 *
 * // Tool 配置
 * ConfigKey.tool("tool_001", "category")
 *
 * // Tool 被 Skill 引用的覆盖配置
 * ConfigKey.toolSkillOverride("tool_001", "skill_123", "timeout")
 *
 * // Workspace 成员配置
 * ConfigKey.workspaceMember("ws_456", "member_001", "role")
 *
 * // Workspace Skill 绑定配置
 * ConfigKey.workspaceSkill("ws_456", "skill_789", "timeout")
 * </pre>
 */
public final class ConfigKey {

    /**
     * 完整配置键格式：{scopeType}:{scopeId}:{targetType}:{targetId}:{configKey}
     */
    private final String scopeType;
    private final String scopeId;
    private final String targetType;
    private final String targetId;
    private final String configKey;

    private ConfigKey(String scopeType, String scopeId, String targetType, String targetId, String configKey) {
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.configKey = configKey;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 兼容旧版 API：创建全局配置键
     *
     * @deprecated 使用 {@link #global(String)} 代替
     */
    @Deprecated
    public static ConfigKey of(String key) {
        if (key == null) {
            return new ConfigKey("GLOBAL", "-", "self", "-", "*");
        }
        return new ConfigKey("GLOBAL", "-", "self", "-", key);
    }

    /**
     * 兼容旧版 API：创建命名空间配置键
     *
     * @deprecated 使用语义化工厂方法代替
     */
    @Deprecated
    public static ConfigKey of(String namespace, String key) {
        if (key == null) {
            // 获取该命名空间下所有配置
            return new ConfigKey(namespace.toUpperCase(), "*", "self", "-", "*");
        }
        return new ConfigKey(namespace.toUpperCase(), "-", "self", "-", key);
    }

    /**
     * 兼容旧版 API：创建完整维度配置键
     *
     * @deprecated 使用语义化工厂方法代替
     */
    @Deprecated
    public static ConfigKey of(String namespace, String scopeType, String scopeId, String key) {
        if ("draft".equals(namespace)) {
            // 草稿键：draft:configId -> DRAFT:-:self:-:configId
            return new ConfigKey("DRAFT", "-", "self", "-", scopeId);
        }
        if (scopeId == null || scopeId.equals("*")) {
            // 获取该 scopeType 下所有配置
            return new ConfigKey(scopeType.toUpperCase(), "*", "self", "-", "*");
        }
        if (key == null) {
            // 获取该 scopeType:scopeId 下所有配置
            return new ConfigKey(scopeType.toUpperCase(), scopeId, "self", "-", "*");
        }
        // 完整配置键
        return new ConfigKey(scopeType.toUpperCase(), scopeId, "self", "-", key);
    }

    /**
     * 全局配置：GLOBAL:-:self:-:{key}
     */
    public static ConfigKey global(String key) {
        return new ConfigKey(ConfigScopeType.GLOBAL.name(), "-", "self", "-", key);
    }

    /**
     * Studio 用户级配置
     */
    public static ConfigKey studio(String studioId, String key) {
        return new ConfigKey(ConfigScopeType.STUDIO.name(), studioId, "self", "-", key);
    }

    /**
     * Workspace 自有配置
     */
    public static ConfigKey workspace(String workspaceId, String key) {
        return new ConfigKey(ConfigScopeType.WORKSPACE.name(), workspaceId, "self", "-", key);
    }

    /**
     * Workspace 引用资产的覆盖配置（最高优先级）
     *
     * @param workspaceId Workspace ID
     * @param targetType 被引用资产类型（CHARACTER, SKILL, OBSERVER, TOOL）
     * @param targetId 被引用资产 ID
     * @param key 配置键
     */
    public static ConfigKey workspaceRefOverride(String workspaceId, String targetType, String targetId, String key) {
        return new ConfigKey(ConfigScopeType.WORKSPACE_REF_OVERRIDE.name(), workspaceId, targetType, targetId, key);
    }

    /**
     * Character 资产配置
     */
    public static ConfigKey character(String characterId, String key) {
        return new ConfigKey(ConfigScopeType.CHARACTER.name(), characterId, "self", "-", key);
    }

    /**
     * Memory 配置
     *
     * @param memoryId Memory ID
     * @param ownerType 归属者类型（CHARACTER 或 WORKSPACE）
     * @param ownerId 归属者 ID
     * @param key 配置键
     */
    public static ConfigKey memory(String memoryId, String ownerType, String ownerId, String key) {
        return new ConfigKey(ConfigScopeType.MEMORY.name(), memoryId, "self", "-", key)
                .withOwner(ownerType, ownerId);
    }

    /**
     * Observer 资产配置
     */
    public static ConfigKey observer(String observerId, String key) {
        return new ConfigKey(ConfigScopeType.OBSERVER.name(), observerId, "self", "-", key);
    }

    /**
     * Skill 资产配置
     */
    public static ConfigKey skill(String skillId, String key) {
        return new ConfigKey(ConfigScopeType.SKILL.name(), skillId, "self", "-", key);
    }

    /**
     * Tool 资产配置
     */
    public static ConfigKey tool(String toolId, String key) {
        return new ConfigKey(ConfigScopeType.TOOL.name(), toolId, "self", "-", key);
    }

    /**
     * Tool 被 Skill 引用的覆盖配置
     *
     * @param toolId Tool ID
     * @param skillId 引用该 Tool 的 Skill ID
     * @param key 配置键
     */
    public static ConfigKey toolSkillOverride(String toolId, String skillId, String key) {
        return new ConfigKey(ConfigScopeType.TOOL.name(), toolId, "SKILL", skillId, key);
    }

    /**
     * Workspace 成员配置
     *
     * @param workspaceId Workspace ID
     * @param memberId 成员 ID
     * @param key 配置键（如 role, notificationSettings 等）
     */
    public static ConfigKey workspaceMember(String workspaceId, String memberId, String key) {
        return new ConfigKey(ConfigScopeType.MEMBER.name(), workspaceId, memberId, "-", key);
    }

    /**
     * Workspace Skill 绑定配置
     *
     * @param workspaceId Workspace ID
     * @param skillId Skill ID
     * @param key 配置键（如 enabled, executionOrder, timeout 等）
     */
    public static ConfigKey workspaceSkill(String workspaceId, String skillId, String key) {
        return new ConfigKey(ConfigScopeType.SKILL.name(), workspaceId, "WORKSPACE_SKILL", skillId, key);
    }

    // ==================== 解析方法 ====================

    /**
     * 从完整配置键字符串解析
     *
     * @param fullKey 完整配置键，格式：{scopeType}:{scopeId}:{targetType}:{targetId}:{configKey}
     */
    public static ConfigKey parse(String fullKey) {
        if (fullKey == null || fullKey.isEmpty()) {
            throw new IllegalArgumentException("ConfigKey fullKey cannot be null or empty");
        }
        String[] parts = fullKey.split(":", 5);
        if (parts.length != 5) {
            throw new IllegalArgumentException("ConfigKey must have 5 parts separated by ':', got: " + fullKey);
        }
        return new ConfigKey(parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    /**
     * 构建完整配置键字符串
     */
    public String toFullKey() {
        return scopeType + ":" + scopeId + ":" + targetType + ":" + targetId + ":" + configKey;
    }

    // ==================== 便捷方法 ====================

    /**
     * 创建带归属信息的副本（用于 Memory）
     */
    private ConfigKey withOwner(String ownerType, String ownerId) {
        // 返回新的 ConfigKey，但通过 setOwnerType/setOwnerId 设置归属
        // 注意：这里需要单独处理，因为 ConfigKey 是不可变的
        // Memory 的 owner 信息需要存储在 ConfigEntity 中
        return this;
    }

    // ==================== 粒度判断 ====================

    /**
     * 是否为全局粒度
     */
    public boolean isGlobal() {
        return ConfigScopeType.GLOBAL.name().equals(scopeType);
    }

    /**
     * 是否为 Studio 粒度
     */
    public boolean isStudio() {
        return ConfigScopeType.STUDIO.name().equals(scopeType);
    }

    /**
     * 是否为 Workspace 粒度
     */
    public boolean isWorkspace() {
        return ConfigScopeType.WORKSPACE.name().equals(scopeType);
    }

    /**
     * 是否为 Workspace 引用覆盖
     */
    public boolean isWorkspaceRefOverride() {
        return ConfigScopeType.WORKSPACE_REF_OVERRIDE.name().equals(scopeType);
    }

    /**
     * 是否为资产粒度（Character/Skill/Observer/Tool）
     */
    public boolean isAsset() {
        return ConfigScopeType.CHARACTER.name().equals(scopeType)
                || ConfigScopeType.SKILL.name().equals(scopeType)
                || ConfigScopeType.OBSERVER.name().equals(scopeType)
                || ConfigScopeType.TOOL.name().equals(scopeType);
    }

    /**
     * 是否为 Memory 粒度
     */
    public boolean isMemory() {
        return ConfigScopeType.MEMORY.name().equals(scopeType);
    }

    /**
     * 是否为引用配置（targetType != self）
     */
    public boolean isReference() {
        return !"self".equals(targetType);
    }

    // ==================== Getter ====================

    public String getScopeType() {
        return scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getKey() {
        return configKey;
    }

    /**
     * @deprecated Use {@link #toFullKey()} instead
     */
    @Deprecated
    public String getConfigKey() {
        return configKey;
    }

    // ==================== Object 方法 ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigKey configKey1 = (ConfigKey) o;
        return Objects.equals(scopeType, configKey1.scopeType)
                && Objects.equals(scopeId, configKey1.scopeId)
                && Objects.equals(targetType, configKey1.targetType)
                && Objects.equals(targetId, configKey1.targetId)
                && Objects.equals(configKey, configKey1.configKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scopeType, scopeId, targetType, targetId, configKey);
    }

    @Override
    public String toString() {
        return toFullKey();
    }
}
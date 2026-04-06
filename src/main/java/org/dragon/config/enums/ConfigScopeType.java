package org.dragon.config.enums;

/**
 * 配置作用域类型
 *
 * <p>用于区分不同粒度的配置管理，一张 Config 表通过 scopeType 字段区分不同作用域。
 *
 * <p>继承链顺序（从低到高）：
 * GLOBAL &lt; STUDIO &lt; WORKSPACE &lt; ASSET &lt; workspace_ref_override
 */
public enum ConfigScopeType {

    /**
     * 全局配置：系统级配置，如 JWT、LLM Provider 等凭证
     */
    GLOBAL,

    /**
     * 用户级配置：用户偏好设置、默认参数等
     */
    STUDIO,

    /**
     * 工作空间配置：Workspace 自身的配置
     */
    WORKSPACE,

    /**
     * Character 资产配置
     */
    CHARACTER,

    /**
     * 记忆配置：隶属于 Character 或 Workspace
     */
    MEMORY,

    /**
     * Observer 配置
     */
    OBSERVER,

    /**
     * 成员配置：Workspace 成员相关配置（存储时 key 格式：WORKSPACE:ws:MEMBER:memberId:xxx）
     */
    MEMBER,

    /**
     * Skill 配置
     */
    SKILL,

    /**
     * Tool 配置
     */
    TOOL,

    /**
     * Workspace 引用覆盖：Workspace 引用资产时的覆盖配置
     * 优先级最高，用于 workspace_ref_override 场景
     */
    WORKSPACE_REF_OVERRIDE
}
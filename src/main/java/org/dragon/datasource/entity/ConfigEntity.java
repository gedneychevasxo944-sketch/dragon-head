package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.config.enums.ScopeBits;

import java.time.LocalDateTime;

/**
 * 配置实体（扁平化存储）
 *
 * <p>使用 scopeBits 位掩码表示激活的层级，各层级 ID 打平存储。
 *
 * <p>存储示例：
 * <ul>
 *   <li>Global: scopeBits=GLOBAL, globalId="-", 其他ID=null</li>
 *   <li>Workspace: scopeBits=GLOBAL|WORKSPACE, workspaceId="ws1", 其他ID=null</li>
 *   <li>Workspace+Character: scopeBits=GLOBAL|WORKSPACE|CHARACTER, workspaceId="ws1", characterId="char1"</li>
 *   <li>Workspace+Character+Tool: scopeBits=GLOBAL|WORKSPACE|CHARACTER|TOOL, workspaceId="ws1", characterId="char1", toolId="tool1"</li>
 * </ul>
 *
 * <p>scopeBits 位定义：
 * <ul>
 *   <li>Bit 0 (1): GLOBAL - 全局配置</li>
 *   <li>Bit 1 (2): STUDIO - 用户/租户级别</li>
 *   <li>Bit 2 (4): WORKSPACE - 工作空间级别</li>
 *   <li>Bit 3 (8): CHARACTER - 角色级别</li>
 *   <li>Bit 4 (16): TOOL - 工具级别</li>
 *   <li>Bit 5 (32): SKILL - 技能级别</li>
 *   <li>Bit 6 (64): MEMORY - 记忆级别</li>
 *   <li>Bit 7 (128): OBSERVER - 观察者级别</li>
 *   <li>Bit 8 (256): MEMBER - 成员级别</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_store", indexes = {
        @Index(name = "idx_scope_bits", columnList = "scope_bits"),
        @Index(name = "idx_config_key", columnList = "config_key"),
        @Index(name = "idx_workspace_character", columnList = "workspace_id, character_id")
})
public class ConfigEntity {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    /**
     * 层级位掩码，表示激活的层级组合
     * 例如: GLOBAL|WORKSPACE|CHARACTER = 0b00001101 = 13
     */
    @Column(name = "scope_bits", nullable = false)
    private Integer scopeBits;

    // ==================== 扁平化层级 ID 存储 ====================

    /**
     * GLOBAL 时使用，固定为 "-"
     */
    @Column(name = "global_id", length = 64)
    private String globalId;

    /**
     * STUDIO 时使用
     */
    @Column(name = "studio_id", length = 64)
    private String studioId;

    /**
     * WORKSPACE 时使用
     */
    @Column(name = "workspace_id", length = 64)
    private String workspaceId;

    /**
     * CHARACTER 时使用
     */
    @Column(name = "character_id", length = 64)
    private String characterId;

    /**
     * TOOL 时使用
     */
    @Column(name = "tool_id", length = 64)
    private String toolId;

    /**
     * SKILL 时使用
     */
    @Column(name = "skill_id", length = 64)
    private String skillId;

    /**
     * MEMORY 时使用
     */
    @Column(name = "memory_id", length = 64)
    private String memoryId;

    /**
     * OBSERVER 时使用
     */
    @Column(name = "observer_id", length = 64)
    private String observerId;

    /**
     * MEMBER 时使用
     */
    @Column(name = "member_id", length = 64)
    private String memberId;

    // ==================== 配置值 ====================

    /**
     * 配置键（只存 key 名字，如 "maxSteps"）
     */
    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    /**
     * 配置值（JSON 格式）
     */
    @DbJson
    @Column(name = "config_value", columnDefinition = "JSON")
    private Object configValue;

    /**
     * 值类型：STRING, NUMBER, BOOLEAN, LIST, OBJECT
     */
    @Column(name = "value_type", length = 32)
    private String valueType;

    /**
     * 状态：DRAFT, PUBLISHED
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PUBLISHED";

    /**
     * 版本号
     */
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    /**
     * 发布时间
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 发布人
     */
    @Column(name = "published_by", length = 100)
    private String publishedBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== 辅助方法 ====================

    /**
     * 检查是否包含 GLOBAL
     */
    public boolean isGlobal() {
        return scopeBits != null && ScopeBits.hasBit(scopeBits, ScopeBits.GLOBAL);
    }

    /**
     * 检查是否包含 WORKSPACE
     */
    public boolean isWorkspace() {
        return scopeBits != null && ScopeBits.hasBit(scopeBits, ScopeBits.WORKSPACE);
    }

    /**
     * 检查是否包含 CHARACTER
     */
    public boolean isCharacter() {
        return scopeBits != null && ScopeBits.hasBit(scopeBits, ScopeBits.CHARACTER);
    }

    /**
     * 检查是否包含 TOOL
     */
    public boolean isTool() {
        return scopeBits != null && ScopeBits.hasBit(scopeBits, ScopeBits.TOOL);
    }

    /**
     * 检查是否包含 SKILL
     */
    public boolean isSkill() {
        return scopeBits != null && ScopeBits.hasBit(scopeBits, ScopeBits.SKILL);
    }

    /**
     * 构建数据库主键
     */
    public static String buildId(int scopeBits, String workspaceId, String characterId,
                                   String toolId, String skillId, String configKey) {
        return String.format("%d:%s:%s:%s:%s:%s",
                scopeBits,
                workspaceId != null ? workspaceId : "",
                characterId != null ? characterId : "",
                toolId != null ? toolId : "",
                skillId != null ? skillId : "",
                configKey);
    }
}

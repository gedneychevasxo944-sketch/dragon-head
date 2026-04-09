package org.dragon.datasource.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.config.enums.ConfigLevel;

import java.time.LocalDateTime;

/**
 * 配置实体（简化版）
 *
 * <p>使用固定的 scopeBit 标识粒度，各层级 ID 扁平化存储。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_store", indexes = {
        @Index(name = "idx_scope_bit", columnList = "scope_bit"),
        @Index(name = "idx_config_key", columnList = "config_key"),
        @Index(name = "idx_lookup", columnList = "scope_bit, workspace_id, character_id, tool_id, skill_id, memory_id, config_key")
})
public class ConfigEntity {

    @Id
    @Column(name = "id", length = 255)
    private String id;

    /**
     * 粒度标识（1-30）
     */
    @Column(name = "scope_bit", nullable = false)
    private Integer scopeBit;

    // ==================== 扁平化层级 ID 存储 ====================

    @Column(name = "workspace_id", length = 64)
    private String workspaceId;

    @Column(name = "character_id", length = 64)
    private String characterId;

    @Column(name = "tool_id", length = 64)
    private String toolId;

    @Column(name = "skill_id", length = 64)
    private String skillId;

    @Column(name = "memory_id", length = 64)
    private String memoryId;

    // ==================== 配置值 ====================

    @Column(name = "config_key", nullable = false, length = 255)
    private String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "value_type", length = 32)
    private String valueType;

    // ==================== 元数据字段 ====================

    /**
     * 配置项名称
     */
    @Column(name = "name", length = 128)
    private String name;

    /**
     * 配置项描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 校验规则（JSON 数组格式）
     */
    @Column(name = "validation_rules", columnDefinition = "JSON")
    private String validationRules;

    /**
     * 枚举选项（JSON 数组格式，用于 ENUM 类型）
     */
    @Column(name = "options", columnDefinition = "JSON")
    private String options;

    /**
     * 修改人
     */
    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    // ==================== 状态和版本 ====================

    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "PUBLISHED";

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ==================== 辅助方法 ====================

    /**
     * 获取配置粒度
     */
    public ConfigLevel getLevel() {
        return ConfigLevel.fromScopeBit(scopeBit);
    }

    /**
     * 构建数据库主键
     */
    public static String buildId(Integer scopeBit, String workspaceId, String characterId,
                                 String toolId, String skillId, String memoryId, String configKey) {
        validateScopeBitFields(scopeBit, workspaceId, characterId, toolId, skillId, memoryId);
        return String.format("%d:%s:%s:%s:%s:%s:%s",
                scopeBit,
                workspaceId != null ? workspaceId : "",
                characterId != null ? characterId : "",
                toolId != null ? toolId : "",
                skillId != null ? skillId : "",
                memoryId != null ? memoryId : "",
                configKey);
    }

    /**
     * 验证 scopeBit 与字段是否匹配
     *
     * @throws IllegalArgumentException 当 scopeBit 包含某层级但对应字段为空时
     */
    public static void validateScopeBitFields(Integer scopeBit, String workspaceId, String characterId,
                                              String toolId, String skillId, String memoryId) {
        if (scopeBit == null) {
            return;
        }
        ConfigLevel level = ConfigLevel.fromScopeBit(scopeBit);
        if (level == null) {
            return;
        }
        if (level.hasWorkspace() && isBlank(workspaceId)) {
            throw new IllegalArgumentException("scopeBit 包含 WORKSPACE，但 workspaceId 为空");
        }
        if (level.hasCharacter() && isBlank(characterId)) {
            throw new IllegalArgumentException("scopeBit 包含 CHARACTER，但 characterId 为空");
        }
        if (level.hasTool() && isBlank(toolId)) {
            throw new IllegalArgumentException("scopeBit 包含 TOOL，但 toolId 为空");
        }
        if (level.hasSkill() && isBlank(skillId)) {
            throw new IllegalArgumentException("scopeBit 包含 SKILL，但 skillId 为空");
        }
        if (level.hasMemory() && isBlank(memoryId)) {
            throw new IllegalArgumentException("scopeBit 包含 MEMORY，但 memoryId 为空");
        }
    }

    private static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (id == null) {
            id = buildId(scopeBit, workspaceId, characterId, toolId, skillId, memoryId, configKey);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
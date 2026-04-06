package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ConfigDefinition 配置项定义实体
 *
 * <p>存储配置项的元数据，包括：
 * <ul>
 *   <li>配置项所属的作用域（scope_type）</li>
 *   <li>配置键名称（config_key）</li>
 *   <li>值类型（value_type）：NUMBER, STRING, BOOLEAN, LIST, OBJECT</li>
 *   <li>描述信息（description）</li>
 *   <li>默认值（default_value）：对应 hardcoded 默认值</li>
 * </ul>
 *
 * <p>config_key 格式：{scopeType}:{configKey}（不含 scopeId/target）
 * <p>示例：
 * <ul>
 *   <li>CHARACTER:maxSteps</li>
 *   <li>OBSERVER:optimizationThreshold</li>
 *   <li>GLOBAL:jwt.access-token-validity</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "config_definitions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_scope_key", columnNames = {"scope_type", "config_key"})
}, indexes = {
        @Index(name = "idx_def_scope_type", columnList = "scope_type"),
        @Index(name = "idx_def_key", columnList = "config_key")
})
public class ConfigDefinition {

    @Id
    private String id;

    /**
     * 作用域类型：GLOBAL, STUDIO, CHARACTER, OBSERVER, SKILL, TOOL, WORKSPACE, MEMORY, MEMBER
     */
    @Column(name = "scope_type", nullable = false, length = 32)
    private String scopeType;

    /**
     * 配置键（不含 scopeId/target 前缀）
     * 格式：{scopeType}:{configKey}
     * 示例：CHARACTER:maxSteps
     */
    @Column(name = "config_key", nullable = false, length = 128)
    private String configKey;

    /**
     * 值类型：NUMBER, STRING, BOOLEAN, LIST, OBJECT
     */
    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType;

    /**
     * 配置项描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 默认值（JSON 格式）
     * 对应代码中的 hardcoded 默认值
     */
    @DbJson
    @Column(name = "default_value", columnDefinition = "JSON")
    private Object defaultValue;

    /**
     * 版本号
     */
    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

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
}
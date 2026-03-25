package org.dragon.skill.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.model.SkillSource;

import java.time.LocalDateTime;

/**
 * Skill 数据库实体。
 * 用于持久化管理 Skill 元数据。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillEntity {

    /** 数据库主键 ID */
    Long id;

    /** Skill 名称，全局唯一 */
    String name;

    /** 来源：BUNDLED, MANAGED, WORKSPACE, EXTRA, PLUGIN */
    SkillSource source;

    /** 分类 */
    SkillCategory category;

    /** 版本号，每次更新自动递增 */
    @Builder.Default
    Integer version = 1;

    /** 标签列表，JSON 字符串存储 */
    String tags;

    /** 管理页面填写的简介 */
    String description;

    /** Skill 文件存放目录（绝对路径） */
    String skillDir;

    /**
     * 人工可控的启用/禁用状态。
     * true=可用，false=禁用（禁用后不会被加载到运行时注册表）。
     * 与运行时加载状态解耦：enabled=false 时，即使文件完好也不加载。
     */
    @Builder.Default
    Boolean enabled = true;

    /**
     * 归属的工作空间 ID。
     * 0L = 系统内置 Skill，对所有工作空间可见。
     * 非 0 = 归属于特定工作空间，仅在该工作空间的 Agent 中加载。
     */
    @Builder.Default
    Long workspaceId = 0L;

    /** 创建时间 */
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    /** 更新时间 */
    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    /** 软删除时间戳，非 null 表示已删除 */
    LocalDateTime deletedAt;
}

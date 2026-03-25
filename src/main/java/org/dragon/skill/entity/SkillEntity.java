package org.dragon.skill.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "skill")
public class SkillEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128, unique = true)
    private String name;

    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private SkillSource source;

    @Column(nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private SkillCategory category;

    @Column(nullable = false)
    @Builder.Default
    Integer version = 1;

    @Column(length = 512)
    private String tags;

    /**
     * 管理页面填写的简介（人工描述，区别于 SKILL.md 中的 description）。
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 存储后端路径标识。
     * 本地模式：/data/skills/{workspaceId}/{skillName}/{version}
     * S3 模式：s3://{bucket}/skills/{workspaceId}/{skillName}/{version}
     */
    @Column(nullable = false, length = 1024)
    private String storagePath;

    /**
     * 来自 SKILL.md frontmatter 的 description。
     * 供 LLM 判断是否调用此 Skill，上传时解析写入，不可人工修改。
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String skillDescription;

    /**
     * SKILL.md frontmatter 之后的正文内容。
     * 注入 LLM System Prompt 的核心内容，上传时解析写入。
     */
    @Column(columnDefinition = "TEXT")
    private String skillContent;

    /**
     * SKILL.md requires 字段的 JSON 序列化。
     * 加载时反序列化为 SkillRequires 对象进行依赖检查。
     */
    @Column(columnDefinition = "TEXT")
    private String requiresConfig;

    /**
     * SKILL.md install 字段的 JSON 序列化（数组）。
     * 依赖缺失时提供安装指引。
     */
    @Column(columnDefinition = "TEXT")
    private String installConfig;

    /**
     * SKILL.md frontmatter 原始 YAML 字符串。
     * 保留完整原始内容，便于后续扩展解析新字段时无需重新下载文件。
     */
    @Column(columnDefinition = "TEXT")
    private String frontmatterRaw;

    /**
     * 人工可控的启用/禁用状态。
     * true=可用，false=禁用（禁用后不会被加载到运行时注册表）。
     * 与运行时加载状态解耦：enabled=false 时，即使文件完好也不加载。
     */
    @Column(nullable = false)
    @Builder.Default
    Boolean enabled = true;

    /**
     * 归属的工作空间 ID。
     * 0L = 系统内置 Skill，对所有工作空间可见。
     * 非 0 = 归属于特定工作空间，仅在该工作空间的 Agent 中加载。
     */
    @Column(nullable = false)
    @Builder.Default
    Long workspaceId = 0L;

    @Column(nullable = false)
    @Builder.Default
    LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * 软删除时间戳，非 null 表示已删除
     */
    private LocalDateTime deletedAt;
}
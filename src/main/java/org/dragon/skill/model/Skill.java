package org.dragon.skill.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已加载的技能定义。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    /** 数据库主键 ID */
    @Builder.Default
    Long id = null;
    /** 版本号 */
    @Builder.Default
    Integer version = 1;
    /** 技能名称（通常是目录名） */
    String name;
    /** 简短描述（来自 frontmatter） */
    String description;
    /** 技能加载来源 */
    SkillSource source;

    /**
     * 存储后端路径标识（来自数据库 storage_path 字段）。
     * 本地模式：/data/skills/{workspaceId}/{skillName}/{version}
     * S3 模式：s3://{bucket}/skills/{workspaceId}/{skillName}/{version}
     * 执行阶段由 SkillExecutionPrepareService 使用此路径下载文件。
     */
    String storagePath;

    /** 包含该技能的目录路径 */
    String baseDir;
    /** SKILL.md 的完整内容（frontmatter 之后的正文） */
    String content;
}
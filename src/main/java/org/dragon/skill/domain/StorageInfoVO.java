package org.dragon.skill.domain;

import lombok.Data;

import java.util.List;

/**
 * storage_info JSON 字段的 Java 映射。
 * <p>
 * S3 模式：bucket + basePath + rootFilePath + files
 * local 模式：仅 rootFilePath（其余为 null）
 * </p>
 *
 * S3 路径约定：skills/{skillId}/v{version}/
 * 示例：
 *   bucket        = "agent-skills"
 *   basePath      = "skills/abc-123/v2"
 *   rootFilePath  = "skills/abc-123/v2/SKILL.md"
 */
@Data
public class StorageInfoVO {

    /** S3 bucket 名称；local 模式为 null */
    private String bucket;

    /** 版本目录路径（不含文件名） */
    private String basePath;

    /** SKILL.md 完整存储路径 */
    private String rootFilePath;

    /** 该版本下所有文件列表 */
    private List<SkillFileItem> files;

    @Data
    public static class SkillFileItem {
        /** 相对于 basePath 的路径，如 "SKILL.md"、"schema/input.json" */
        private String path;
        /** 文件字节大小 */
        private Long size;
        /** 文件类型标签：markdown / json / yaml / text */
        private String type;
    }
}


package org.dragon.workspace.material;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Material 物料实体
 * 任务执行所需的静态资源，如文件、图片、知识库文档等
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    /**
     * 可见性范围
     */
    public enum VisibilityScope {
        /**
         * 仅本次任务可见
         */
        TASK_SCOPED,
        /**
         * 全 Workspace 可见
         */
        WORKSPACE_SCOPED,
        /**
         * 仅指定 Character 可见
         */
        CHARACTER_SCOPED
    }

    /**
     * 物料唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 可见性范围
     */
    @Builder.Default
    private VisibilityScope visibility = VisibilityScope.WORKSPACE_SCOPED;

    /**
     * 可见性目标 ID（根据 visibility 不同指向 taskId 或 characterId）
     */
    private String visibilityTargetId;

    /**
     * 物料名称
     */
    private String name;

    /**
     * 物料大小（字节）
     */
    private long size;

    /**
     * 物料类型（MIME type）
     */
    private String type;

    /**
     * 存储键（用于 MaterialStorage 检索）
     */
    private String storageKey;

    /**
     * 上传者 ID
     */
    private String uploader;

    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 物料种类（DOCUMENT, IMAGE, AUDIO, VIDEO, OTHER）
     */
    private String kind;

    /**
     * 解析状态（PENDING, PARSING, SUCCESS, FAILED）
     */
    private String parseStatus;

    /**
     * 解析内容 ID（关联 ParsedMaterialContent）
     */
    private String parsedContentId;

    /**
     * 来源渠道
     */
    private String sourceChannel;

    /**
     * 来源消息 ID
     */
    private String sourceMessageId;
}

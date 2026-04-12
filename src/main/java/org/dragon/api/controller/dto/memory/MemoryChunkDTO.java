package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 记忆片段 DTO
 * 一个 chunk 对应一个完整的 markdown 文件
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryChunkDTO {

    /** 片段唯一标识符 */
    private String id;

    /** 所属数据源 ID */
    private String sourceId;

    /** 片段标题 */
    private String title;

    /** 片段内容（文件全文） */
    private String content;

    /** 内容摘要 */
    private String summary;

    /** 标签列表 */
    private List<String> tags;

    /** 索引状态：indexed / pending / failed */
    private String indexedStatus;

    /** 关联片段 ID 列表 */
    private List<String> relations;

    /** 文件路径 */
    private String filePath;

    /** 文件类型：markdown / json / text / other */
    private String fileType;

    /** 文件大小（字节） */
    private Long totalSize;

    /** 同步状态：synced / syncing / pending / failed / disabled */
    private String syncStatus;

    /** 健康状态：healthy / warning / error / unknown */
    private String healthStatus;

    /** 最后同步时间 */
    private Instant lastSyncAt;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
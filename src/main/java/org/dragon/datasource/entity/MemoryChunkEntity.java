package org.dragon.datasource.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 记忆片段实体类
 * 一个 chunk 对应一个完整的 markdown 文件，挂载在指定数据源下
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Entity
@Table(name = "memory_chunk")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryChunkEntity {

    /**
     * chunk 唯一标识符
     */
    @Id
    private String id;

    /**
     * 所属数据源 ID
     */
    private String sourceId;

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段内容（文件全文，TEXT 类型）
     */
    private String content;

    /**
     * 片段摘要（TEXT 类型）
     */
    private String summary;

    /**
     * 标签列表（JSON 序列化，如 ["tag1","tag2"]）
     */
    private String tags;

    /**
     * 索引状态：pending / indexed / failed
     */
    private String indexedStatus;

    /**
     * 关联 chunk ID 列表（JSON 序列化，如 ["chunk-1","chunk-2"]）
     */
    private String relations;

    /**
     * 文件路径（文件系统路径或相对路径）
     */
    private String filePath;

    /**
     * 文件类型：markdown / json / text / other
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long totalSize;

    /**
     * 同步状态：synced / syncing / pending / failed / disabled
     */
    private String syncStatus;

    /**
     * 健康状态：healthy / warning / error / unknown
     */
    private String healthStatus;

    /**
     * 最后同步时间
     */
    private Instant lastSyncAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
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
 * 记忆绑定关系实体类
 * 记录 chunk 文件与 character/workspace 记忆空间的绑定关系
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Entity
@Table(name = "mem_binding")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingEntity {

    /**
     * 绑定关系唯一标识符
     */
    @Id
    private String id;

    /**
     * 关联的文件 ID
     */
    private String fileId;

    /**
     * 关联的 chunk ID
     */
    private String chunkId;

    /**
     * 绑定目标类型：character / workspace
     */
    private String targetType;

    /**
     * 绑定目标 ID
     */
    private String targetId;

    /**
     * 绑定目标名称
     */
    private String targetName;

    /**
     * 挂载类型：full / selective / rule
     */
    private String mountType;

    /**
     * 生成的快照文件路径，相对于记忆根目录，如 mem/xxx.md
     */
    private String snapshotFileName;

    /**
     * 数据源 ID
     */
    private String sourceId;

    /**
     * 对应记忆条目 ID
     */
    private String memoryId;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}
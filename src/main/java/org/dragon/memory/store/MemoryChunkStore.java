package org.dragon.memory.store;

import org.dragon.datasource.entity.MemoryChunkEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 记忆片段存储接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryChunkStore extends Store {

    /**
     * 保存记忆片段
     *
     * @param entity 记忆片段实体
     */
    void save(MemoryChunkEntity entity);

    /**
     * 根据 ID 查找记忆片段
     *
     * @param id 片段 ID
     * @return 记忆片段实体
     */
    Optional<MemoryChunkEntity> findById(String id);

    /**
     * 根据数据源 ID 查找记忆片段列表
     *
     * @param sourceId 数据源 ID
     * @return 记忆片段列表
     */
    List<MemoryChunkEntity> findBySourceId(String sourceId);

    /**
     * 按条件查找记忆片段列表
     *
     * @param sourceId      数据源 ID（可为 null）
     * @param syncStatus    同步状态（可为 null）
     * @param indexedStatus 索引状态（可为 null）
     * @param search        内容关键词（可为 null）
     * @return 记忆片段列表
     */
    List<MemoryChunkEntity> findByCondition(String sourceId, String syncStatus, String indexedStatus, String search);

    /**
     * 删除记忆片段
     *
     * @param id 片段 ID
     * @return 是否成功
     */
    boolean deleteById(String id);

    /**
     * 批量删除记忆片段
     *
     * @param ids 片段 ID 列表
     * @return 是否成功
     */
    boolean deleteBatch(List<String> ids);

    /**
     * 按条件统计记忆片段数量
     *
     * @param sourceId      数据源 ID（可为 null）
     * @param syncStatus    同步状态（可为 null）
     * @param indexedStatus 索引状态（可为 null）
     * @param search        内容关键词（可为 null）
     * @return 数量
     */
    long countByCondition(String sourceId, String syncStatus, String indexedStatus, String search);

    /**
     * 更新单个片段的索引状态
     *
     * @param id     片段 ID
     * @param status 目标状态
     */
    void updateIndexStatus(String id, String status);

    /**
     * 批量更新索引状态
     *
     * @param ids    片段 ID 列表
     * @param status 目标状态
     */
    void updateIndexStatusBatch(List<String> ids, String status);

    /**
     * 更新单个片段的同步状态
     *
     * @param id         片段 ID
     * @param syncStatus 目标同步状态
     */
    void updateSyncStatus(String id, String syncStatus);
}
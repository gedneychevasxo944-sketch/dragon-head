package org.dragon.memory.store;

import org.dragon.datasource.entity.SourceDocumentEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 数据源存储接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface SourceDocumentStore extends Store {
    /**
     * 保存数据源
     *
     * @param entity 数据源实体
     */
    void save(SourceDocumentEntity entity);

    /**
     * 根据 ID 查找数据源
     *
     * @param id 数据源 ID
     * @return 数据源实体
     */
    Optional<SourceDocumentEntity> findById(String id);

    /**
     * 查找所有数据源
     *
     * @return 数据源实体列表
     */
    List<SourceDocumentEntity> findAll();

    /**
     * 根据条件查找数据源
     *
     * @param search     搜索关键词
     * @param status     状态过滤
     * @param sourceType 源类型过滤
     * @return 数据源实体列表
     */
    List<SourceDocumentEntity> findByCondition(String search, String status, String sourceType);

    /**
     * 删除数据源
     *
     * @param id 数据源 ID
     * @return 是否成功
     */
    boolean deleteById(String id);

    /**
     * 统计数据源数量
     *
     * @param search     搜索关键词
     * @param status     状态过滤
     * @param sourceType 源类型过滤
     * @return 数据源数量
     */
    long countByCondition(String search, String status, String sourceType);
}

package org.dragon.memory.store;

import org.dragon.datasource.entity.BindingEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 绑定关系存储接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface BindingStore extends Store {

    /**
     * 保存绑定关系
     *
     * @param entity 绑定关系实体
     */
    void save(BindingEntity entity);

    /**
     * 根据 ID 查找绑定关系
     *
     * @param id 绑定关系 ID
     * @return 绑定关系实体
     */
    Optional<BindingEntity> findById(String id);

    /**
     * 根据目标类型和目标 ID 查找绑定关系列表
     *
     * @param targetType 目标类型（character/workspace）
     * @param targetId   目标 ID
     * @return 绑定关系列表
     */
    List<BindingEntity> findByTarget(String targetType, String targetId);

    /**
     * 根据文件 ID 查找绑定关系列表
     *
     * @param fileId 文件 ID
     * @return 绑定关系列表
     */
    List<BindingEntity> findByFileId(String fileId);

    /**
     * 删除绑定关系
     *
     * @param id 绑定关系 ID
     * @return 是否成功
     */
    boolean deleteById(String id);

    /**
     * 统计指定目标的绑定关系数量
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @return 绑定关系数量
     */
    long countByTarget(String targetType, String targetId);
}
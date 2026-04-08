package org.dragon.asset.store;

import org.dragon.datasource.entity.AssetPublishStatusEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * AssetPublishStatusStore 资产发布状态存储接口
 *
 * <p>管理资产的草稿/发布/归档状态
 */
public interface AssetPublishStatusStore extends Store {

    /**
     * 保存发布状态
     *
     * @param entity 发布状态实体
     */
    void save(AssetPublishStatusEntity entity);

    /**
     * 更新发布状态
     *
     * @param entity 发布状态实体
     */
    void update(AssetPublishStatusEntity entity);

    /**
     * 根据 ID 查询
     *
     * @param id ID
     * @return Optional 发布状态实体
     */
    Optional<AssetPublishStatusEntity> findById(String id);

    /**
     * 根据资源类型和资源 ID 查询
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return Optional 发布状态实体
     */
    Optional<AssetPublishStatusEntity> findByResource(String resourceType, String resourceId);

    /**
     * 查询指定资源类型下所有指定状态的实体
     *
     * @param resourceType 资源类型
     * @param status 状态
     * @return 发布状态实体列表
     */
    List<AssetPublishStatusEntity> findByResourceTypeAndStatus(String resourceType, String status);

    /**
     * 查询指定资源类型下所有实体
     *
     * @param resourceType 资源类型
     * @return 发布状态实体列表
     */
    List<AssetPublishStatusEntity> findByResourceType(String resourceType);

    /**
     * 查询所有指定状态的实体
     *
     * @param status 状态
     * @return 发布状态实体列表
     */
    List<AssetPublishStatusEntity> findByStatus(String status);

    /**
     * 检查是否存在
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 是否存在
     */
    boolean exists(String resourceType, String resourceId);

    /**
     * 删除发布状态
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     */
    void delete(String resourceType, String resourceId);
}
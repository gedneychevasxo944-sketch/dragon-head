package org.dragon.expert.store;

import org.dragon.permission.enums.ResourceType;
import org.dragon.store.Store;
import org.dragon.datasource.entity.ExpertEntity;

import java.util.List;
import java.util.Optional;

/**
 * ExpertStore Expert 标记存储接口
 *
 * @author yijunw
 */
public interface ExpertStore extends Store {

    /**
     * 保存 Expert 标记
     *
     * @param expertMark Expert 标记实体
     */
    void save(ExpertEntity expertMark);

    /**
     * 更新 Expert 标记
     *
     * @param expertMark Expert 标记实体
     */
    void update(ExpertEntity expertMark);

    /**
     * 根据 ID 查询
     *
     * @param id 主键 ID
     * @return Expert 标记实体
     */
    Optional<ExpertEntity> findById(String id);

    /**
     * 根据资源类型和资源 ID 查询
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return Expert 标记实体
     */
    Optional<ExpertEntity> findByResource(ResourceType resourceType, String resourceId);

    /**
     * 查询全部 Expert 标记
     *
     * @return Expert 标记列表
     */
    List<ExpertEntity> findAll();

    /**
     * 根据资源类型查询
     *
     * @param resourceType 资源类型
     * @return Expert 标记列表
     */
    List<ExpertEntity> findByResourceType(ResourceType resourceType);

    /**
     * 根据分类查询
     *
     * @param category 分类
     * @return Expert 标记列表
     */
    List<ExpertEntity> findByCategory(String category);

    /**
     * 增加派生次数
     *
     * @param id 主键 ID
     */
    void incrementUsageCount(String id);

    /**
     * 删除 Expert 标记
     *
     * @param id 主键 ID
     */
    void delete(String id);

    /**
     * 根据资源类型和资源 ID 删除
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     */
    void deleteByResource(ResourceType resourceType, String resourceId);
}
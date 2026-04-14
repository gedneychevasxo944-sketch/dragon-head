package org.dragon.asset.store;

import org.dragon.asset.enums.AssociationType;
import org.dragon.datasource.entity.AssetAssociationEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * AssetAssociationStore 资产关联存储接口
 */
public interface AssetAssociationStore extends Store {

    /**
     * 保存关联记录
     */
    void save(AssetAssociationEntity association);

    /**
     * 更新关联记录
     */
    void update(AssetAssociationEntity association);

    /**
     * 删除关联记录
     */
    void delete(Long id);

    /**
     * 根据ID查询
     */
    Optional<AssetAssociationEntity> findById(Long id);

    /**
     * 根据源资产查询关联
     */
    List<AssetAssociationEntity> findBySource(AssociationType type, ResourceType sourceType, String sourceId);

    /**
     * 根据目标资产查询关联
     */
    List<AssetAssociationEntity> findByTarget(AssociationType type, ResourceType targetType, String targetId);

    /**
     * 根据关联类型查询
     */
    List<AssetAssociationEntity> findByType(AssociationType type);

    /**
     * 检查关联是否存在
     */
    boolean exists(AssociationType type, ResourceType sourceType, String sourceId,
                   ResourceType targetType, String targetId);

    /**
     * 根据源资产和目标资产删除关联
     */
    void deleteBySourceAndTarget(AssociationType type, ResourceType sourceType, String sourceId,
                                 ResourceType targetType, String targetId);

    /**
     * 启用/禁用指定关联关系（通过五元组定位）
     *
     * @param type       关联类型
     * @param sourceType 源资产类型
     * @param sourceId   源资产 ID
     * @param targetType 目标资产类型
     * @param targetId   目标资产 ID
     * @param enabled    true=启用，false=禁用
     */
    void setEnabled(AssociationType type, ResourceType sourceType, String sourceId,
                    ResourceType targetType, String targetId, boolean enabled);
}

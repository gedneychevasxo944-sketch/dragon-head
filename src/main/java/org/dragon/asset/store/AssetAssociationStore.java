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
}

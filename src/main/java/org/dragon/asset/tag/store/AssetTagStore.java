package org.dragon.asset.tag.store;

import org.dragon.datasource.entity.AssetTagEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Set;

/**
 * AssetTagStore 资产标签存储接口
 * <p>
 * 标签一定伴随资产绑定，无独立 CRUD。
 * 每行 PK = (resource_type, resource_id, name)。
 */
public interface AssetTagStore extends Store {

    /**
     * 保存标签（幂等：PK 相同则覆盖）
     */
    void save(AssetTagEntity tag);

    /**
     * 删除标签
     */
    void delete(String resourceType, String resourceId, String name);

    /**
     * 按资产查询标签
     */
    List<AssetTagEntity> findByResource(String resourceType, String resourceId);

    /**
     * 批量按资产 ID 列表查询（避免 N+1）
     */
    List<AssetTagEntity> findByResources(String resourceType, List<String> resourceIds);

    /**
     * 按资产类型查询所有标签（去重，返回标签名集合）
     */
    Set<String> findTagNamesByResourceType(String resourceType);

    /**
     * 查询某资产的某标签是否存在
     */
    boolean exists(String resourceType, String resourceId, String name);

    /**
     * 按标签名和资产类型查询（用于 TraitService 按标签名过滤）
     */
    List<AssetTagEntity> findByTagNameAndResourceType(String tagName, String resourceType);
}

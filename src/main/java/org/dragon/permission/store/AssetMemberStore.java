package org.dragon.permission.store;

import org.dragon.permission.entity.AssetMemberEntity;
import org.dragon.store.Store;
import org.dragon.permission.enums.ResourceType;

import java.util.List;
import java.util.Optional;

/**
 * AssetMemberStore 资产成员存储接口
 */
public interface AssetMemberStore extends Store {

    /**
     * 保存成员关系
     */
    void save(AssetMemberEntity member);

    /**
     * 更新成员关系
     */
    void update(AssetMemberEntity member);

    /**
     * 删除成员关系
     */
    void delete(Long id);

    /**
     * 根据 ID 获取成员
     */
    Optional<AssetMemberEntity> findById(Long id);

    /**
     * 根据资源类型和资源ID获取所有成员
     */
    List<AssetMemberEntity> findByResource(ResourceType resourceType, String resourceId);

    /**
     * 根据资源类型、资源ID和用户ID获取成员
     */
    Optional<AssetMemberEntity> findByResourceAndUser(ResourceType resourceType, String resourceId, Long userId);

    /**
     * 根据用户ID获取所有成员关系
     */
    List<AssetMemberEntity> findByUserId(Long userId);

    /**
     * 查找用户待接受的邀请
     */
    List<AssetMemberEntity> findPendingInvitationsByUserId(Long userId);

    /**
     * 删除资源的所有成员关系
     */
    void deleteByResource(ResourceType resourceType, String resourceId);

    /**
     * 检查成员关系是否存在
     */
    boolean exists(ResourceType resourceType, String resourceId, Long userId);
}

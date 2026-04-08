package org.dragon.notification.store;

import org.dragon.datasource.entity.NotificationEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * NotificationStore 通知存储接口
 */
public interface NotificationStore extends Store {

    /**
     * 保存通知
     */
    void save(NotificationEntity notification);

    /**
     * 更新通知
     */
    void update(NotificationEntity notification);

    /**
     * 删除通知
     */
    void delete(String id);

    /**
     * 根据ID查找
     */
    Optional<NotificationEntity> findById(String id);

    /**
     * 根据用户ID查找通知（按时间倒序）
     */
    List<NotificationEntity> findByUserId(Long userId);

    /**
     * 根据用户ID查找未读通知
     */
    List<NotificationEntity> findUnreadByUserId(Long userId);

    /**
     * 根据用户ID和类型查找
     */
    List<NotificationEntity> findByUserIdAndType(Long userId, String type);

    /**
     * 获取用户未读通知数量
     */
    long countUnreadByUserId(Long userId);

    /**
     * 将用户所有通知标记为已读
     */
    void markAllAsReadByUserId(Long userId);

    /**
     * 删除用户的所有通知
     */
    void deleteByUserId(Long userId);
}

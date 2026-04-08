package org.dragon.notification.store;

import io.ebean.Database;
import org.dragon.datasource.entity.NotificationEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MySqlNotificationStore 通知MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlNotificationStore implements NotificationStore {

    private final Database mysqlDb;

    public MySqlNotificationStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(NotificationEntity notification) {
        mysqlDb.save(notification);
    }

    @Override
    public void update(NotificationEntity notification) {
        mysqlDb.update(notification);
    }

    @Override
    public void delete(String id) {
        mysqlDb.delete(NotificationEntity.class, id);
    }

    @Override
    public Optional<NotificationEntity> findById(String id) {
        NotificationEntity entity = mysqlDb.find(NotificationEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public List<NotificationEntity> findByUserId(Long userId) {
        return mysqlDb.find(NotificationEntity.class)
                .where()
                .eq("userId", userId)
                .orderBy()
                .desc("createdAt")
                .findList();
    }

    @Override
    public List<NotificationEntity> findUnreadByUserId(Long userId) {
        return mysqlDb.find(NotificationEntity.class)
                .where()
                .eq("userId", userId)
                .eq("isRead", false)
                .orderBy()
                .desc("createdAt")
                .findList();
    }

    @Override
    public List<NotificationEntity> findByUserIdAndType(Long userId, String type) {
        return mysqlDb.find(NotificationEntity.class)
                .where()
                .eq("userId", userId)
                .eq("type", type)
                .orderBy()
                .desc("createdAt")
                .findList();
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        return mysqlDb.find(NotificationEntity.class)
                .where()
                .eq("userId", userId)
                .eq("isRead", false)
                .findCount();
    }

    @Override
    public void markAllAsReadByUserId(Long userId) {
        mysqlDb.find(NotificationEntity.class)
                .where()
                .eq("userId", userId)
                .eq("isRead", false)
                .findList()
                .forEach(n -> {
                    n.setIsRead(true);
                    mysqlDb.update(n);
                });
    }

    @Override
    public void deleteByUserId(Long userId) {
        mysqlDb.find(NotificationEntity.class)
                .where()
                .eq("userId", userId)
                .delete();
    }
}

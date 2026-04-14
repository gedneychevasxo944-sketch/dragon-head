package org.dragon.user.store;

import io.ebean.Database;
import org.dragon.datasource.entity.UserEntity;
import org.dragon.store.StoreType;
import org.dragon.store.StoreTypeAnn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * MySqlUserStore 用户MySQL存储实现
 */
@Component
@StoreTypeAnn(StoreType.MYSQL)
public class MySqlUserStore implements UserStore {

    private final Database mysqlDb;

    public MySqlUserStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(UserEntity user) {
        mysqlDb.save(user);
    }

    @Override
    public void update(UserEntity user) {
        // Fetch existing entity first to preserve fields not being updated
        UserEntity existing = mysqlDb.find(UserEntity.class, user.getId());
        if (existing != null) {
            mergeIfNotNull(user, existing);
        }
        mysqlDb.update(user);
    }

    private void mergeIfNotNull(UserEntity target, UserEntity source) {
        if (target.getUsername() == null) {
            target.setUsername(source.getUsername());
        }
        if (target.getPhone() == null) {
            target.setPhone(source.getPhone());
        }
        if (target.getPasswordHash() == null) {
            target.setPasswordHash(source.getPasswordHash());
        }
        if (target.getNickname() == null) {
            target.setNickname(source.getNickname());
        }
        if (target.getAvatar() == null) {
            target.setAvatar(source.getAvatar());
        }
        if (target.getStatus() == null) {
            target.setStatus(source.getStatus());
        }
        if (target.getLastLoginAt() == null) {
            target.setLastLoginAt(source.getLastLoginAt());
        }
        if (target.getLastLoginIp() == null) {
            target.setLastLoginIp(source.getLastLoginIp());
        }
        if (target.getLoginFailCount() == null) {
            target.setLoginFailCount(source.getLoginFailCount());
        }
        if (target.getLockUntil() == null) {
            target.setLockUntil(source.getLockUntil());
        }
    }

    @Override
    public Optional<UserEntity> findById(Long id) {
        UserEntity entity = mysqlDb.find(UserEntity.class, id);
        return Optional.ofNullable(entity);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return mysqlDb.find(UserEntity.class)
                .where()
                .eq("username", username)
                .findOneOrEmpty();
    }

    @Override
    public Optional<UserEntity> findByPhone(String phone) {
        return mysqlDb.find(UserEntity.class)
                .where()
                .eq("phone", phone)
                .findOneOrEmpty();
    }
}

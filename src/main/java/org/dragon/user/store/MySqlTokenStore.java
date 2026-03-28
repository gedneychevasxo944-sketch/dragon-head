package org.dragon.user.store;

import io.ebean.Database;
import org.dragon.user.entity.UserTokenEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MySqlTokenStore Token MySQL存储实现
 */
@Component
public class MySqlTokenStore implements TokenStore {

    private final Database mysqlDb;

    public MySqlTokenStore(@Qualifier("mysqlEbeanDatabase") Database mysqlDb) {
        this.mysqlDb = mysqlDb;
    }

    @Override
    public void save(UserTokenEntity token) {
        mysqlDb.save(token);
    }

    @Override
    public Optional<UserTokenEntity> findByRefreshToken(String refreshToken) {
        return mysqlDb.find(UserTokenEntity.class)
                .where()
                .eq("refreshToken", refreshToken)
                .findOneOrEmpty();
    }

    @Override
    public void deleteById(String id) {
        mysqlDb.delete(UserTokenEntity.class, id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        mysqlDb.find(UserTokenEntity.class)
                .where()
                .eq("userId", userId)
                .delete();
    }

    @Override
    public void deleteExpired(LocalDateTime before) {
        mysqlDb.find(UserTokenEntity.class)
                .where()
                .lt("expiresAt", before)
                .delete();
    }
}

package org.dragon.user.store;

import org.dragon.datasource.entity.UserTokenEntity;
import org.dragon.store.Store;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * TokenStore Token存储接口
 */
public interface TokenStore extends Store {

    /**
     * 保存Token
     */
    void save(UserTokenEntity token);

    /**
     * 根据refreshToken查找
     */
    Optional<UserTokenEntity> findByRefreshToken(String refreshToken);

    /**
     * 删除用户的某个Token
     */
    void deleteById(String id);

    /**
     * 删除用户的所有Token
     */
    void deleteByUserId(Long userId);

    /**
     * 删除过期的Token
     */
    void deleteExpired(LocalDateTime before);
}

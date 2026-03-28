package org.dragon.user.store;

import org.dragon.user.entity.UserEntity;

import java.util.Optional;

/**
 * UserStore 用户存储接口
 */
public interface UserStore {

    /**
     * 保存用户
     */
    void save(UserEntity user);

    /**
     * 更新用户
     */
    void update(UserEntity user);

    /**
     * 根据ID查找用户
     */
    Optional<UserEntity> findById(Long id);

    /**
     * 根据用户名查找用户
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * 根据手机号查找用户
     */
    Optional<UserEntity> findByPhone(String phone);
}

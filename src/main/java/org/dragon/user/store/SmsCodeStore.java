package org.dragon.user.store;

import org.dragon.user.entity.SmsCodeEntity;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SmsCodeStore 短信验证码存储接口
 */
public interface SmsCodeStore {

    /**
     * 保存验证码
     */
    void save(SmsCodeEntity smsCode);

    /**
     * 查找最新未使用的验证码
     */
    Optional<SmsCodeEntity> findLatest(String phone, String type);

    /**
     * 标记验证码已使用
     */
    void markUsed(String id);

    /**
     * 删除过期的验证码
     */
    void deleteExpired(LocalDateTime before);
}

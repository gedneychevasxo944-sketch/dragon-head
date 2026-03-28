package org.dragon.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SmsCodeEntity 短信验证码实体
 * 映射数据库 sms_code 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sms_code")
public class SmsCodeEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 32)
    private String phone;

    @Column(nullable = false, length = 8)
    private String code;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "create_time", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 验证码类型枚举
     */
    public enum Type {
        LOGIN,          // 登录
        BIND_PHONE,     // 绑定手机
        FORGET_PASSWORD // 忘记密码
    }

    /**
     * 检查验证码是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查验证码是否可用
     */
    public boolean isUsable() {
        return !used && !isExpired();
    }
}

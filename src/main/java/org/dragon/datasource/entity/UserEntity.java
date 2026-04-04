package org.dragon.datasource.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserEntity 用户实体
 * 映射数据库 adeptify_user 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "adeptify_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(unique = true, length = 32)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 128)
    private String nickname;

    @Column(length = 512)
    private String avatar;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "NORMAL";

    /**
     * 用户角色：ADMIN（管理员）或 USER（普通用户）
     */
    @Column(length = 16)
    @Builder.Default
    private String role = "USER";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    @Column(name = "login_fail_count")
    @Builder.Default
    private Integer loginFailCount = 0;

    @Column(name = "lock_until")
    private LocalDateTime lockUntil;

    @Column(name = "create_time", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time", nullable = false)
    @Builder.Default
    private LocalDateTime updateTime = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 用户状态枚举
     */
    public enum Status {
        NORMAL,
        DISABLED,
        DELETED
    }

    /**
     * 检查账户是否被锁定
     */
    public boolean isLocked() {
        return lockUntil != null && LocalDateTime.now().isBefore(lockUntil);
    }

    /**
     * 检查账户是否可用
     */
    public boolean isActive() {
        return status.equals("NORMAL") && !isLocked();
    }
}

package org.dragon.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserTokenEntity 用户Token实体
 * 映射数据库 user_token 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_token")
public class UserTokenEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "create_time", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
}

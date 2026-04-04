package org.dragon.datasource.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.permission.enums.Role;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * PermissionPolicyEntity 权限策略表
 * 存储角色 + 资源类型 对应的权限集合
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission_policy")
public class PermissionPolicyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 资源类型，'*' 表示所有资源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    /**
     * 权限集合，JSON 格式存储
     */
    @Column(nullable = false, columnDefinition = "JSON")
    private String permission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
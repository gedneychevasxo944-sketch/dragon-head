-- V9: Create RBAC tables (asset_member and permission_policy)
-- This migration creates the unified permission system

-- ============================================================================
-- asset_member: User's membership/role on an asset
-- ============================================================================
CREATE TABLE asset_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_type VARCHAR(32) NOT NULL COMMENT 'WORKSPACE, CHARACTER, SKILL, etc.',
    resource_id VARCHAR(64) NOT NULL COMMENT 'ID of the resource',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role VARCHAR(32) NOT NULL COMMENT 'OWNER, ADMIN, COLLABORATOR, MEMBER',
    invited_by VARCHAR(64) COMMENT 'User ID who invited this member',
    invited_at DATETIME COMMENT 'When the invitation was sent',
    accepted_at DATETIME COMMENT 'When the invitation was accepted',
    accepted BOOLEAN DEFAULT FALSE COMMENT 'Whether invitation is accepted',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_asset_member (resource_type, resource_id, user_id),
    INDEX idx_asset_member_resource (resource_type, resource_id),
    INDEX idx_asset_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- permission_policy: Permissions granted by role+resource_type
-- ============================================================================
CREATE TABLE permission_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_type VARCHAR(32) NOT NULL COMMENT 'Specific type or * for all',
    role VARCHAR(32) NOT NULL COMMENT 'OWNER, ADMIN, COLLABORATOR, MEMBER',
    permission JSON NOT NULL COMMENT 'Array of permissions',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_policy (role, resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Seed default permission policies
-- ============================================================================

-- ============================================================================
-- OWNER: 所有者，拥有全部权限
-- ============================================================================
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('*', 'OWNER', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR", "TRANSFER"]');

-- ============================================================================
-- ADMIN: 管理员，拥有编辑管理权限
-- ============================================================================
-- Workspace: 管理员可以查看、使用、编辑、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WORKSPACE', 'ADMIN', '["VIEW", "USE", "EDIT", "MANAGE_COLLABORATOR"]');

-- Character: 管理员可以查看、使用、编辑、删除、发布、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('CHARACTER', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]');

-- Skill (PERSONAL): 管理员可以查看、使用、编辑、删除、发布、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('SKILL', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]');

-- Tool: 管理员可以查看、使用、编辑、删除、发布、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('TOOL', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]');

-- Observer: 管理员可以查看、使用、编辑、删除、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('OBSERVER', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "MANAGE_COLLABORATOR"]');

-- Config: 管理员可以查看、使用、编辑、删除
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('CONFIG', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE"]');

-- Model: 管理员可以查看、使用、编辑、删除、发布、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('MODEL', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]');

-- Template: 管理员可以查看、使用、编辑、删除、发布、管理成员
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('TEMPLATE', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE", "PUBLISH", "MANAGE_COLLABORATOR"]');

-- Commonsense: 管理员可以查看、使用、编辑、删除
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('COMMONSENSE', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE"]');

-- Trait: 管理员可以查看、使用、编辑、删除
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('TRAIT', 'ADMIN', '["VIEW", "USE", "EDIT", "DELETE"]');

-- ============================================================================
-- COLLABORATOR: 协作者，仅能查看使用
-- ============================================================================
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('*', 'COLLABORATOR', '["VIEW", "USE"]');

-- ============================================================================
-- MEMBER: 成员，仅能查看（Workspace内资产）
-- ============================================================================
-- Workspace 成员可以查看
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('WORKSPACE', 'MEMBER', '["VIEW"]');

-- Character 成员可以查看
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('CHARACTER', 'MEMBER', '["VIEW"]');

-- Observer 成员可以查看
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('OBSERVER', 'MEMBER', '["VIEW"]');

-- Config 成员可以查看
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('CONFIG', 'MEMBER', '["VIEW"]');

-- Commonsense 成员可以查看
INSERT INTO permission_policy (resource_type, role, permission) VALUES
('COMMONSENSE', 'MEMBER', '["VIEW"]');

-- ============================================================================
-- USER: 普通认证用户（无资产权限，通过创建资产获得 OWNER 角色）
-- ============================================================================
-- USER 角色不设置资产级权限，通过系统规则（如创建资产）获得 OWNER 角色

-- ============================================================================
-- Migrate existing asset_collaborator data to asset_member
-- ============================================================================
INSERT INTO asset_member (resource_type, resource_id, user_id, role, invited_by, invited_at, accepted_at, accepted)
SELECT
    resource_type,
    resource_id,
    collaborator_id,
    'COLLABORATOR',
    invited_by,
    invited_at,
    accepted_at,
    accepted
FROM asset_collaborator;

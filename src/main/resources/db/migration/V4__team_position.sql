-- ============================================================================
-- V4: 团队席位表 (team_position)
-- ============================================================================

-- Team position table
CREATE TABLE IF NOT EXISTS team_position (
    id VARCHAR(128) PRIMARY KEY COMMENT '唯一标识 (workspaceId_roleName)',
    workspace_id VARCHAR(64) NOT NULL COMMENT 'Workspace ID',
    role_name VARCHAR(128) NOT NULL COMMENT '角色名称',
    role_package VARCHAR(128) COMMENT '角色包/分类',
    purpose TEXT COMMENT '岗位目的/职责',
    scope TEXT COMMENT '岗位范围',
    assigned_character_id VARCHAR(64) COMMENT '分配的 Character ID（空表示空缺）',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    INDEX idx_position_workspace (workspace_id),
    INDEX idx_position_character (assigned_character_id),
    UNIQUE KEY uk_workspace_role (workspace_id, role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队席位表';

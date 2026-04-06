-- ============================================================================
-- V101: 资产发布状态表
-- 支持 CHARACTER, SKILL, OBSERVER, MODEL, TEMPLATE 等资产的草稿/发布/归档状态
-- ============================================================================

CREATE TABLE IF NOT EXISTS asset_publish_status (
    id VARCHAR(64) PRIMARY KEY COMMENT '主键 UUID',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型：CHARACTER, SKILL, OBSERVER, MODEL, TEMPLATE',
    resource_id VARCHAR(64) NOT NULL COMMENT '资源 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT, PUBLISHED, ARCHIVED',
    version INT DEFAULT 1 COMMENT '发布版本号',
    published_at DATETIME COMMENT '发布时间',
    published_by VARCHAR(100) COMMENT '发布人 ID',
    archived_at DATETIME COMMENT '归档时间',
    archived_by VARCHAR(100) COMMENT '归档人 ID',
    snapshot JSON COMMENT '发布时的资产快照',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',

    UNIQUE KEY uk_aps_resource (resource_type, resource_id),
    INDEX idx_aps_status (status),
    INDEX idx_aps_published_by (published_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
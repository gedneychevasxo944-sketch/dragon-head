-- ============================================================================
-- V104: Asset Association Table
-- 资产关联表，用于维护资产之间的关系
-- ============================================================================

CREATE TABLE IF NOT EXISTS `asset_association` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `association_type` VARCHAR(32) NOT NULL COMMENT '关联类型：CHARACTER_WORKSPACE, MEMORY_CHARACTER, MEMORY_WORKSPACE, TOOL_SKILL, OBSERVER_WORKSPACE',
    `source_type` VARCHAR(32) NOT NULL COMMENT '源资产类型',
    `source_id` VARCHAR(64) NOT NULL COMMENT '源资产ID',
    `target_type` VARCHAR(32) NOT NULL COMMENT '目标资产类型',
    `target_id` VARCHAR(64) NOT NULL COMMENT '目标资产ID',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_association` (`association_type`, `source_type`, `source_id`, `target_type`, `target_id`),
    INDEX `idx_source` (`source_type`, `source_id`),
    INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资产关联表';

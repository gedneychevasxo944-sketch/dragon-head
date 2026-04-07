-- ============================================================================
-- V105: Notification Table
-- 通知表，用于存储用户通知
-- ============================================================================

CREATE TABLE IF NOT EXISTS `notification` (
    `id` VARCHAR(64) NOT NULL COMMENT '主键(UUID)',
    `user_id` BIGINT NOT NULL COMMENT '通知接收用户ID',
    `type` VARCHAR(32) NOT NULL COMMENT '通知类型：APPROVAL_REQUEST, APPROVAL_RESULT, COLLABORATOR_INVITE, SYSTEM',
    `title` VARCHAR(128) NOT NULL COMMENT '通知标题',
    `content` TEXT COMMENT '通知内容',
    `link` VARCHAR(256) COMMENT '点击跳转链接',
    `source_type` VARCHAR(32) COMMENT '来源类型（如 APPROVAL）',
    `source_id` VARCHAR(64) COMMENT '来源ID（如审批请求ID）',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_notification_user` (`user_id`),
    INDEX `idx_notification_user_type` (`user_id`, `type`),
    INDEX `idx_notification_user_read` (`user_id`, `is_read`),
    INDEX `idx_notification_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

package org.dragon.asset.enums;

/**
 * 资产发布状态
 */
public enum PublishStatus {
    /**
     * 草稿状态
     */
    DRAFT,

    /**
     * 待审批状态（发布审批进行中）
     */
    PENDING,

    /**
     * 已发布状态
     */
    PUBLISHED,

    /**
     * 已归档状态
     */
    ARCHIVED
}
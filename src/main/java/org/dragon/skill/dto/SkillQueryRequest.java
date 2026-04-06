package org.dragon.skill.dto;

import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import lombok.Data;

/**
 * Skill 列表页检索请求参数。
 *
 * <p>所有过滤字段均为可选，不传则不过滤；分页参数有默认值。
 *
 * <pre>
 * 支持的过滤维度：
 *   - keyword：模糊匹配 name / displayName / description
 *   - status：按生命周期状态过滤（draft / active / disabled，不含 deleted）
 *   - category：按分类过滤
 *   - visibility：按可见性过滤
 *   - creatorId：按创建者过滤（适合"我的技能"场景）
 * 排序：
 *   - sortBy：name / createdAt / publishedAt / usageCount（默认 createdAt）
 *   - sortOrder：asc / desc（默认 desc）
 * </pre>
 */
@Data
public class SkillQueryRequest {

    // ── 过滤条件 ────────────────────────────────────────────────────

    /**
     * 关键字，模糊搜索 name / displayName / description。
     * 空或 null 则不过滤。
     */
    private String keyword;

    /**
     * 生命周期状态过滤。
     * null 表示查询全部（deleted 记录始终不对外暴露，由 Service 层强制排除）。
     */
    private SkillStatus status;

    /**
     * 分类过滤。null 表示全部分类。
     */
    private SkillCategory category;

    /**
     * 可见性过滤。null 表示不限制。
     */
    private SkillVisibility visibility;

    /**
     * 按创建者 ID 过滤（适合"我的技能"场景）。
     * null 表示不限创建者。
     */
    private Long creatorId;

    // ── 排序 ────────────────────────────────────────────────────────

    /**
     * 排序字段。
     * 可选值：name / createdAt / publishedAt / usageCount（默认 createdAt）。
     */
    private String sortBy = "createdAt";

    /**
     * 排序方向：asc / desc（默认 desc）。
     */
    private String sortOrder = "desc";

    // ── 分页 ────────────────────────────────────────────────────────

    /**
     * 页码，从 1 开始（默认 1）。
     */
    private int page = 1;

    /**
     * 每页数量（默认 20，最大 100）。
     */
    private int pageSize = 20;

    // ── 辅助方法 ─────────────────────────────────────────────────────

    /** 转换为 Store 层使用的 offset */
    public int getOffset() {
        return Math.max(0, (page - 1)) * getClampedPageSize();
    }

    /** 防止 pageSize 超限 */
    public int getClampedPageSize() {
        return Math.min(Math.max(1, pageSize), 100);
    }
}


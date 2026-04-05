package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页结果包装。
 *
 * @param <T> 列表项类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> items;

    /** 符合条件的总记录数（去重 skillId） */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页数量 */
    private int pageSize;

    /** 总页数 */
    public int getTotalPages() {
        if (pageSize <= 0) return 0;
        return (int) Math.ceil((double) total / pageSize);
    }

    /** 是否还有下一页 */
    public boolean isHasNext() {
        return page < getTotalPages();
    }

    public static <T> PageResult<T> of(List<T> items, long total, int page, int pageSize) {
        return new PageResult<>(items, total, page, pageSize);
    }
}


package org.dragon.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页响应包装类
 *
 * @param <T> 数据项类型
 * @author zhz
 * @version 1.0
 */
@Data
public class PageResponse<T> {

    /** 数据列表 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private int page;

    /** 每页数量 */
    private int pageSize;

    /**
     * 构建分页响应
     *
     * @param list     数据列表
     * @param total    总条数
     * @param page     当前页
     * @param pageSize 每页数量
     * @param <T>      数据类型
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(List<T> list, long total, int page, int pageSize) {
        PageResponse<T> resp = new PageResponse<>();
        resp.setList(list);
        resp.setTotal(total);
        resp.setPage(page);
        resp.setPageSize(pageSize);
        return resp;
    }

    /**
     * 构建简单分页响应（total = list.size()）
     *
     * @param list 数据列表
     * @param <T>  数据类型
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(List<T> list) {
        return of(list, list == null ? 0 : list.size(), 1, list == null ? 0 : list.size());
    }
}
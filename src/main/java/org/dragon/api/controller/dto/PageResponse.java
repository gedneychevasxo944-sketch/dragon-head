package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总数量
     */
    private long total;

    /**
     * 页码
     */
    private int page;

    /**
     * 每页大小
     */
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
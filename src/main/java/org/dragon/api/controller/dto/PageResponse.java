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
}

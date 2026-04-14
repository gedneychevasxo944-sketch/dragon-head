package org.dragon.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PageResult 统一分页返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> content;

    private int page;

    private int size;

    private long total;

    public static <T> PageResult<T> of(List<T> content, int page, int size, long total) {
        return PageResult.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .total(total)
                .build();
    }
}

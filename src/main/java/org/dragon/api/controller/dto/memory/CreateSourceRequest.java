package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建数据源请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSourceRequest {
    /**
     * 数据源标题
     */
    private String title;

    /**
     * 数据源路径（文件系统路径或 URL）
     */
    private String sourcePath;

    /**
     * 数据源类型：file/url/api/chat
     */
    private String sourceType;

    /**
     * 后端存储位置
     */
    private String backend;

    /**
     * 数据源提供者
     */
    private String provider;

    /**
     * 是否启用（默认 true）
     */
    private Boolean enabled;
}

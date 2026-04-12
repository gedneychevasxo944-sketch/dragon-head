package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建记忆片段请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChunkRequest {

    /** 所属数据源 ID */
    private String sourceId;

    /** 片段标题 */
    private String title;

    /** 片段内容（文件全文） */
    private String content;

    /** 内容摘要 */
    private String summary;

    /** 标签列表 */
    private List<String> tags;

    /** 文件路径 */
    private String filePath;

    /** 文件类型：markdown / json / text / other */
    private String fileType;

    /** 文件大小（字节） */
    private Long totalSize;
}
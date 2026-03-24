package org.dragon.workspace.material;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已解析的物料内容
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedMaterialContent {

    /**
     * 唯一标识
     */
    private String id;

    /**
     * 关联的物料 ID
     */
    private String materialId;

    /**
     * 提取的纯文本内容
     */
    private String textContent;

    /**
     * 结构化内容（JSON/Map 等）
     */
    private Object structuredContent;

    /**
     * 解析时的元数据
     */
    private Map<String, Object> metadata;

    /**
     * 解析状态
     */
    private ParseStatus status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 解析时间
     */
    private LocalDateTime parsedAt;

    /**
     * 解析状态枚举
     */
    public enum ParseStatus {
        PENDING,
        SUCCESS,
        FAILED
    }
}

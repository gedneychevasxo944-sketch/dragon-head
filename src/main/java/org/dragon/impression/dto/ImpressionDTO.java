package org.dragon.impression.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.impression.enums.ImpressionSentiment;
import org.dragon.impression.enums.ImpressionType;

import java.time.LocalDateTime;

/**
 * ImpressionDTO 印象数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpressionDTO {

    private String id;

    private ImpressionType sourceType;

    private String sourceId;

    private ImpressionType targetType;

    private String targetId;

    private String name;

    private String value;

    private ImpressionSentiment sentiment;

    private int trustLevel;

    private String summary;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

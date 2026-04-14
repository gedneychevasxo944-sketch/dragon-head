package org.dragon.tool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.actionlog.ActionType;

import java.time.LocalDateTime;

/**
 * 工具操作日志 VO，用于 API 返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolActionLog {

    private String id;
    private String toolId;
    private String name;
    private String displayName;
    private ActionType actionType;
    private String actionLabel;
    private String content;
    private String operatorName;
    private Integer version;
    private LocalDateTime createdAt;
}

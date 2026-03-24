package org.dragon.workspace.task.notify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 任务消息载荷
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessagePayload {
    private String taskName;
    private String taskDescription;
    private String status;
    private String progress;
    private String question;
    private String result;
    private String errorMessage;
    private Map<String, Object> metadata;
}

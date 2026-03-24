package org.dragon.workspace.task.notify;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务出站事件
 * 统一封装任务状态变化时的通知消息
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskOutboundEvent {

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 任务 ID
     */
    private String taskId;

    /**
     * 父任务 ID
     */
    private String parentTaskId;

    /**
     * 执行者 Character ID
     */
    private String characterId;

    /**
     * 来源渠道
     */
    private String channel;

    /**
     * 目标 chatId
     */
    private String chatId;

    /**
     * 引用的消息 ID
     */
    private String quoteMessageId;

    /**
     * 消息载荷
     */
    private TaskMessagePayload payload;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        STARTED,
        PROGRESS,
        QUESTION,
        WAITING,
        COMPLETED,
        FAILED
    }
}

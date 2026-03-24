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
    /**
     * 任务名称
     */
    private String taskName;
    /**
     * 任务描述
     */
    private String taskDescription;
    /**
     * 任务状态
     */
    private String status;
    /**
     * 进度信息
     */
    private String progress;
    /**
     * 问题内容（用于追问用户）
     */
    private String question;
    /**
     * 任务结果
     */
    private String result;
    /**
     * 错误信息
     */
    private String errorMessage;
    /**
     * 文本内容（统一文案）
     */
    private String text;
    /**
     * 结构化内容
     */
    private Map<String, Object> structuredContent;
    /**
     * 引用的消息 ID（用于回复/引用）
     */
    private String quoteMessageId;
    /**
     * 是否 @ 用户
     */
    private boolean mentionUser;
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}

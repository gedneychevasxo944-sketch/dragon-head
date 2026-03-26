package org.dragon.workspace.service.task.arrangement.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务创建命令对象
 * 统一封装新任务创建所需的所有参数
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreationCommand {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String taskDescription;

    /**
     * 任务输入
     */
    private Object input;

    /**
     * 创建者 ID
     */
    private String creatorId;

    /**
     * 任务元数据
     */
    private Map<String, Object> metadata;

    /**
     * 来源渠道
     */
    private String sourceChannel;

    /**
     * 来源消息 ID
     */
    private String sourceMessageId;

    /**
     * 来源聊天 ID
     */
    private String sourceChatId;
}
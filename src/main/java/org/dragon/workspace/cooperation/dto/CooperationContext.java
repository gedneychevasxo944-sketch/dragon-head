package org.dragon.workspace.cooperation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 协作上下文
 * 封装 Workspace 协作所需的所有上下文信息
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CooperationContext {

    private String workspaceId;
    private String taskId;
    private String sessionId;
    private Map<String, Object> metadata;

    /**
     * 协作类型
     */
    private CooperationType type;

    public enum CooperationType {
        /**
         * 任务协作
         */
        TASK,
        /**
         * 聊天协作
         */
        CHAT,
        /**
         * 决策协作
         */
        DECISION
    }
}
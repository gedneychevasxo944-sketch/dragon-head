package org.dragon.workspace.service;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Character 执行上下文
 * 用于在任务执行时传递必要的上下文信息
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskBridgeContext {
    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * 父任务 ID（可选）
     */
    private String parentTaskId;

    /**
     * 协作会话 ID（可选）
     */
    private String collaborationSessionId;

    /**
     * 同级 Character ID 列表（可选）
     */
    private List<String> peerCharacterIds;

    /**
     * 依赖任务 ID 列表（可选）
     */
    private List<String> dependencyTaskIds;

    /**
     * 最新会话消息列表（可选）
     */
    private List<String> latestSessionMessages;
}

package org.dragon.workspace.cooperation;

import org.dragon.task.Task;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.chat.ChatSession;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;

/**
 * Workspace 协作服务接口
 * 统一管理 Workspace 内的协作行为，包括聊天和任务通知
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceCooperationService {

    /**
     * 获取聊天室
     */
    ChatRoom getChatRoom(String workspaceId);

    /**
     * 获取任务通知器
     */
    WorkspaceTaskNotifier getTaskNotifier();

    /**
     * 通知任务开始
     */
    void notifyTaskStarted(Task task);

    /**
     * 通知任务进度
     */
    void notifyTaskProgress(Task task, String progress);

    /**
     * 通知任务等待中
     */
    void notifyTaskWaiting(Task task, String reason);

    /**
     * 通知任务完成
     */
    void notifyTaskCompleted(Task task);

    /**
     * 通知任务失败
     */
    void notifyTaskFailed(Task task, String errorMessage);

    /**
     * 创建协作会话
     */
    ChatSession createCollaborationSession(String workspaceId, String taskId, java.util.List<String> participantIds);

    /**
     * 标记参与者等待状态
     */
    void markParticipantWaiting(String sessionId, String characterId, String reason);
}
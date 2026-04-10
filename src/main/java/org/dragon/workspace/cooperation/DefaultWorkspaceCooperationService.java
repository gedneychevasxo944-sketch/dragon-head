package org.dragon.workspace.cooperation;

import java.util.List;

import org.dragon.task.Task;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.chat.ChatSession;
import org.dragon.workspace.cooperation.task.CollaborationSessionCoordinator;
import org.dragon.workspace.cooperation.task.notify.WorkspaceTaskNotifier;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Workspace 协作服务默认实现
 * 统一管理 Workspace 内的协作行为
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultWorkspaceCooperationService implements WorkspaceCooperationService {

    private final ChatRoom chatRoom;
    private final WorkspaceTaskNotifier taskNotifier;
    private final CollaborationSessionCoordinator sessionCoordinator;
    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;

    @Override
    public ChatRoom getChatRoom(String workspaceId) {
        return chatRoom;
    }

    @Override
    public WorkspaceTaskNotifier getTaskNotifier() {
        return taskNotifier;
    }

    @Override
    public void notifyTaskStarted(Task task) {
        taskNotifier.notifyStarted(task);
    }

    @Override
    public void notifyTaskProgress(Task task, String progress) {
        taskNotifier.notifyProgress(task, progress);
    }

    @Override
    public void notifyTaskWaiting(Task task, String reason) {
        taskNotifier.notifyWaiting(task, reason);
    }

    @Override
    public void notifyTaskCompleted(Task task) {
        taskNotifier.notifyCompleted(task);
    }

    @Override
    public void notifyTaskFailed(Task task, String errorMessage) {
        taskNotifier.notifyFailed(task, errorMessage);
    }

    @Override
    public ChatSession createCollaborationSession(String workspaceId, String taskId, List<String> participantIds) {
        return chatRoom.startTaskCollaboration(workspaceId, taskId, participantIds);
    }

    @Override
    public void markParticipantWaiting(String sessionId, String characterId, String reason) {
        chatRoom.markParticipantWaiting(sessionId, characterId, reason);
    }
}
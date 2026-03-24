package org.dragon.workspace.service.task.execution;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.task.TaskStore;
import org.dragon.workspace.chat.ChatRoom;
import org.dragon.workspace.chat.ChatSession;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 协作会话协调器
 * 负责创建协作会话并将 sessionId 绑定到父子任务
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationSessionCoordinator {

    private final ChatRoom chatRoom;
    private final TaskStore taskStore;

    /**
     * 创建协作会话并绑定到父子任务
     *
     * @param parentTask 父任务
     * @param childTasks 子任务列表
     * @return 创建的会话
     */
    public ChatSession createAndBindSession(Task parentTask, List<Task> childTasks) {
        // 收集所有子任务的执行者作为参与者
        List<String> participantIds = childTasks.stream()
                .map(Task::getCharacterId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (participantIds.isEmpty()) {
            log.warn("[CollaborationSessionCoordinator] No participants for session, skipping");
            return null;
        }

        // 创建会话
        ChatSession session = chatRoom.createSession(
                parentTask.getWorkspaceId(),
                participantIds,
                parentTask.getId());

        // 绑定 sessionId 到父任务
        parentTask.setCollaborationSessionId(session.getId());
        taskStore.update(parentTask);

        // 绑定 sessionId 到所有子任务
        for (Task childTask : childTasks) {
            childTask.setCollaborationSessionId(session.getId());
            taskStore.update(childTask);
        }

        log.info("[CollaborationSessionCoordinator] Created session {} and bound to {} tasks",
                session.getId(), childTasks.size() + 1);
        return session;
    }
}

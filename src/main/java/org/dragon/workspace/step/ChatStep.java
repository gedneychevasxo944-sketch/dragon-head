package org.dragon.workspace.step;

import java.util.List;
import java.util.Map;

import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.cooperation.chat.ChatMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ChatStep - Character 进入 ChatRoom 协作
 *
 * <p>独立 Step，可多次进入。Character 通过 ChatRoom 发布/订阅消息。
 *
 * @author yijunw
 */
@Slf4j
@RequiredArgsConstructor
public class ChatStep implements Step {

    private final ChatRoom chatRoom;

    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
        long startTime = System.currentTimeMillis();

        // 从上下文中获取任务 ID
        String taskId = ctx.getTask() != null ? ctx.getTask().getId() : null;
        if (taskId == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .output("no task")
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            // 获取最新消息
            List<ChatMessage> messages = chatRoom.getMessages(taskId, 20);

            return StepResult.builder()
                    .stepName(getName())
                    .input(taskId)
                    .output(Map.of("messageCount", messages.size()))
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[ChatStep] Chat step failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}
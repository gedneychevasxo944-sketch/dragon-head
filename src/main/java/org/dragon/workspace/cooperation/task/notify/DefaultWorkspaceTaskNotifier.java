package org.dragon.workspace.cooperation.task.notify;

import java.time.LocalDateTime;

import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.MentionConfig;
import org.dragon.channel.enums.ActionType;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.task.WorkspaceTaskService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认任务通知器实现
 * 将任务状态变化封装为 ActionMessage 并通过 ChannelManager 发送
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class DefaultWorkspaceTaskNotifier implements WorkspaceTaskNotifier {

    private final ChannelManager channelManager;
    private final WorkspaceTaskService workspaceTaskService;

    public DefaultWorkspaceTaskNotifier(@Lazy ChannelManager channelManager,
                                        @Lazy WorkspaceTaskService workspaceTaskService) {
        this.channelManager = channelManager;
        this.workspaceTaskService = workspaceTaskService;
    }

    @Override
    public void notifyStarted(Task task) {
        String content = buildMessage(task, "🚀 任务已开始", "STARTED");
        sendNotification(task, content, ActionType.REPLY);
    }

    @Override
    public void notifyProgress(Task task, String progress) {
        String content = buildMessage(task, "📍 " + progress, "PROGRESS");
        sendNotification(task, content, ActionType.REPLY);
    }

    @Override
    public void notifyQuestion(Task task, String question) {
        String content = buildMessage(task, "❓ " + question, "QUESTION");
        sendNotification(task, content, ActionType.REPLY);
    }

    @Override
    public void notifyWaiting(Task task, String reason) {
        String content = buildMessage(task, "⏳ 等待中: " + reason, "WAITING");
        sendNotification(task, content, ActionType.REPLY);
    }

    @Override
    public void notifyCompleted(Task task) {
        String result = task.getResult() != null ? task.getResult() : "任务已完成";
        String content = buildMessage(task, "✅ " + result, "COMPLETED");
        sendNotification(task, content, ActionType.REPLY);
    }

    @Override
    public void notifyFailed(Task task, String errorMessage) {
        String error = errorMessage != null ? errorMessage : (task.getErrorMessage() != null ? task.getErrorMessage() : "任务执行失败");
        String content = buildMessage(task, "❌ " + error, "FAILED");
        sendNotification(task, content, ActionType.REPLY);
    }

    /**
     * 构建消息内容
     */
    private String buildMessage(Task task, String prefix, String status) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        if (task.getName() != null) {
            sb.append("\n📋 任务: ").append(task.getName());
        }
        if (task.getDescription() != null) {
            sb.append("\n📝 描述: ").append(task.getDescription());
        }
        return sb.toString();
    }

    /**
     * 发送通知
     */
    private void sendNotification(Task task, String content, ActionType actionType) {
        if (task.getWorkspaceId() == null) {
            log.warn("[DefaultWorkspaceTaskNotifier] Cannot notify task {} with no workspaceId", task.getId());
            return;
        }

        // 从 TaskStore 获取任务的 sourceChatId 和 sourceMessageId
        var taskOpt = workspaceTaskService.getTask(task.getWorkspaceId(), task.getId());
        if (taskOpt.isEmpty()) {
            log.warn("[DefaultWorkspaceTaskNotifier] Task {} not found", task.getId());
            return;
        }

        Task currentTask = taskOpt.get();
        String chatId = currentTask.getSourceChatId();
        String quoteMessageId = currentTask.getSourceMessageId();

        if (chatId == null) {
            log.warn("[DefaultWorkspaceTaskNotifier] No chatId for task {}, skipping notification", task.getId());
            return;
        }

        ActionMessage message = new ActionMessage();
        message.setChannelName(currentTask.getSourceChannel() != null ? currentTask.getSourceChannel() : "feishu");
        message.setActionType(actionType);
        message.setQuoteMessageId(quoteMessageId);
        message.setReceiveId(chatId);
        message.setReceiveType("chat");
        message.setMessageType("text");
        message.setContent(content);

        // 如果有发送者，@ 发送者
        if (currentTask.getCreatorId() != null) {
            MentionConfig mentionConfig = new MentionConfig();
            mentionConfig.setMentionOpenId(currentTask.getCreatorId());
            message.setMentionConfig(mentionConfig);
        }

        channelManager.routeMessageOutbound(message);
        log.info("[DefaultWorkspaceTaskNotifier] Sent {} notification for task {}", actionType, task.getId());
    }
}

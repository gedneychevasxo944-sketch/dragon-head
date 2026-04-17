package org.dragon.gateway;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.MentionConfig;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.channel.enums.ActionType;
import org.dragon.channel.service.ChannelBindingService;
import org.dragon.character.CharacterRegistry;
import org.dragon.material.Material;
import org.dragon.task.Task;
import org.dragon.task.TaskExecutionService;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.dragon.workspace.material.WorkspaceMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Gateway 实现
 * 只负责渠道协议转换，将业务执行委托给 WorkspaceService
 *
 * @author zhz
 * @version 1.0
 */
@Component
@Slf4j
public class AgentGateway implements Gateway {

    @Autowired
    @Lazy
    private ChannelManager channelManager;

    @Autowired
    private CharacterRegistry characterRegistry;

    @Autowired
    private ChannelBindingService channelBindingService;

    @Autowired
    @Lazy
    private  WorkspaceMaterialService workspaceMaterialService;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Override
    public void dispatch(NormalizedMessage inboundMsg) {
        // 路由解析：用 (channel, chatId) 查找绑定的 workspaceId
        Optional<String> workspaceIdOpt = channelBindingService.resolveWorkspaceId(inboundMsg.getChannel().getCode(), inboundMsg.getChatId());
        // 将解析结果写回消息，方便下游链路使用
        workspaceIdOpt.ifPresent(inboundMsg::setWorkspaceId);
        if (workspaceIdOpt.isPresent()) {
            // ========== 路径 A：Workspace 多 Agent 编排路径 ==========
            dispatchToWorkspace(inboundMsg, workspaceIdOpt.get());
        } else {
            // ========== 路径 B：单 Character Fallback 路径 ==========
            log.info("[Gateway] No workspace binding found for channel={} chatId={}, falling back to single character", inboundMsg.getChannel().getCode(), inboundMsg.getChatId());
            dispatchToSingleCharacter(inboundMsg);
        }
    }

    /**
     * 执行即时任务（单 Character 路径）。
     */
    public String executeInstantTask(String characterId, String userInput) {
        return taskExecutionService.execute(characterId, userInput);
    }



    /**
     * 路径 A：分发到 Workspace，走多 Agent 智能编排
     */
    private void dispatchToWorkspace(NormalizedMessage inboundMsg, String workspaceId) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("[Gateway] Dispatching to workspace={} from channel={} chatId={}",
                        workspaceId, inboundMsg.getChannel(), inboundMsg.getChatId());

                // 如果消息包含附件，先摄入为物料
                if (inboundMsg.getNormalizedFiles() != null && !inboundMsg.getNormalizedFiles().isEmpty()) {
                    log.info("[Gateway] Found {} files in message, ingesting as materials", inboundMsg.getNormalizedFiles().size());
                    java.util.List<Material> materials = workspaceMaterialService.ingestNormalizedFiles(
                            workspaceId,
                            inboundMsg.getNormalizedFiles(),
                            inboundMsg.getSenderId(),
                            java.util.Map.of("chatId", inboundMsg.getChatId(), "messageId", inboundMsg.getMessageId())
                    );
                    log.info("[Gateway] Ingested {} materials", materials.size());
                }

                // 将用户消息作为任务提交给 TaskExecutionService 执行
                TaskCreationCommand command = TaskCreationCommand.builder()
                        .taskName("用户请求")
                        .taskDescription(inboundMsg.getTextContent())
                        .input(inboundMsg)
                        .creatorId(inboundMsg.getSenderId())
                        .metadata(inboundMsg.getMetadata())
                        .sourceChannel(inboundMsg.getChannel() != null ? inboundMsg.getChannel().getCode() : null)
                        .sourceMessageId(inboundMsg.getMessageId())
                        .sourceChatId(inboundMsg.getChatId())
                        .build();
                Task task = taskExecutionService.submitAndExecute(workspaceId, command);
                log.info("[Gateway] Workspace task submitted, taskId={}", task.getId());
                // Workspace 编排为异步执行，回复由各 Character 通过 ActionMessage 下行推送
                // 此处无需再主动发消息，等待编排层回调
            } catch (Exception e) {
                log.error("[Gateway] Workspace dispatch failed for workspaceId={}", workspaceId, e);
                ActionMessage actionMessage = buildActionMessage(inboundMsg, "任务提交失败: " + e.getMessage());
                channelManager.routeMessageOutbound(actionMessage);
            }
        });
    }

    /**
     * 路径 B：无 Workspace 绑定，Fallback 到单 Character 即时任务
     */

    private void dispatchToSingleCharacter(NormalizedMessage inboundMsg) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 获取默认 Character
                Optional<org.dragon.character.Character> characterOpt = characterRegistry.getDefaultCharacter();

                if (!characterOpt.isPresent()) {
                    log.warn("[Gateway] No default character configured");
                    // Fallback to mock
                    ActionMessage actionMessage = buildActionMessage(inboundMsg, "你刚刚给我发了 '" + inboundMsg.getTextContent() +"' 我选择不回复你");
                    channelManager.routeMessageOutbound(actionMessage);
                    return;
                }

                // 2. 执行即时任务
                String characterId = characterOpt.get().getId();
                String result = executeInstantTask(characterId, inboundMsg.getTextContent());

                // 3. 返回消息
                ActionMessage actionMessage = buildActionMessage(inboundMsg, result);
                channelManager.routeMessageOutbound(actionMessage);

            } catch (Exception e) {
                log.error("[Gateway] Single character dispatch failed", e);
                ActionMessage actionMessage = buildActionMessage(inboundMsg, "处理失败: " + e.getMessage());
                channelManager.routeMessageOutbound(actionMessage);
            }
        });
    }

    /**
     * 构建回复消息
     *
     * @param inboundMsg 收到的消息
     * @param content 回复内容
     * @return 回复消息
     */
    private ActionMessage buildActionMessage(NormalizedMessage inboundMsg, String content) {
        ActionMessage actionMessage = new ActionMessage();
        actionMessage.setChannelName(inboundMsg.getChannel().getCode());
        actionMessage.setActionType(ActionType.REPLY);
        actionMessage.setQuoteMessageId(inboundMsg.getMessageId());
        actionMessage.setMessageType("text");
        actionMessage.setContent(content);

        MentionConfig mentionConfig = new MentionConfig();
        mentionConfig.setMentionOpenId(inboundMsg.getSenderId());
        actionMessage.setMentionConfig(mentionConfig);

        return actionMessage;
    }
}

package org.dragon.gateway;

import lombok.extern.slf4j.Slf4j;
import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.orchestration.OrchestrationService.OrchestrationRequest;
import org.dragon.agent.orchestration.OrchestrationService.OrchestrationResult;
import org.dragon.channel.ChannelManager;
import org.dragon.channel.entity.ActionMessage;
import org.dragon.channel.entity.MentionConfig;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.channel.enums.ActionType;
import org.dragon.character.CharacterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Description:
 * Author: zhz
 * Version: 1.0
 * Create Date Time: 2026/3/13 23:14
 * Update Date Time:
 *
 */
@Component
@Slf4j
public class AgentGateway implements Gateway {

    @Autowired
    @Lazy
    private ChannelManager channelManager;

    @Autowired
    private OrchestrationService orchestrationService;

    @Autowired
    private CharacterRegistry characterRegistry;

    @Autowired
    private ModelRegistry modelRegistry;

    @Override
    public void dispatch(NormalizedMessage inboundMsg) {
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 获取默认 Character
                Optional<org.dragon.character.Character> characterOpt = characterRegistry.getDefaultCharacter();

                if (characterOpt.isEmpty()) {
                    log.warn("[Gateway] No default character configured");
                    // Fallback to mock
                    ActionMessage actionMessage = mockCallLlmBrain(inboundMsg);
                    channelManager.routeMessageOutbound(actionMessage);
                    return;
                }

                String characterId = characterOpt.get().getId();
                String modelId = modelRegistry.getDefault()
                        .map(m -> m.getId())
                        .orElse(null);

                // 2. 通过 OrchestrationService 执行
                OrchestrationRequest request = OrchestrationRequest.builder()
                        .characterId(characterId)
                        .message(inboundMsg.getTextContent())
                        .mode(OrchestrationService.Mode.REACT)
                        .build();

                OrchestrationResult result = orchestrationService.orchestrate(request);

                // 3. 返回消息
                ActionMessage actionMessage = buildActionMessage(inboundMsg, result.getResponse());
                channelManager.routeMessageOutbound(actionMessage);

            } catch (Exception e) {
                log.error("[Gateway] Execution failed", e);
                ActionMessage actionMessage = buildActionMessage(inboundMsg, "处理失败: " + e.getMessage());
                channelManager.routeMessageOutbound(actionMessage);
            }
        });
    }

    private ActionMessage buildActionMessage(NormalizedMessage inboundMsg, String content) {
        ActionMessage actionMessage = new ActionMessage();
        actionMessage.setChannelName("Feishu");
        actionMessage.setActionType(ActionType.REPLY);
        actionMessage.setQuoteMessageId(inboundMsg.getMessageId());
        actionMessage.setMessageType("text");
        actionMessage.setContent(content);

        MentionConfig mentionConfig = new MentionConfig();
        mentionConfig.setMentionOpenId(inboundMsg.getSenderId());
        actionMessage.setMentionConfig(mentionConfig);

        return actionMessage;
    }

    private ActionMessage mockCallLlmBrain(NormalizedMessage inboundMsg) {
        ActionMessage actionMessage = new ActionMessage();
        actionMessage.setChannelName("Feishu");
        actionMessage.setActionType(ActionType.REPLY);
        actionMessage.setQuoteMessageId(inboundMsg.getMessageId());
        actionMessage.setMessageType("text");
        actionMessage.setContent("你刚刚给我发了 '" + inboundMsg.getTextContent() +"' 我选择不回复你");
        MentionConfig mentionConfig = new MentionConfig();
        mentionConfig.setMentionOpenId(inboundMsg.getSenderId());
        actionMessage.setMentionConfig(mentionConfig);
        return actionMessage;
    }

}

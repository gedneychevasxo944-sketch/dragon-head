package org.dragon.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.tool.runtime.adapter.ToolCallRequest;
import org.dragon.tool.service.ToolExecutionService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Action 执行器
 * 负责执行解析后的 Action
 * 支持 TOOL / MEMORY / RESPOND / FINISH / STATUS_CHANGE 类型
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActionExecutor {

    private final ToolExecutionService toolExecutionService;
    private final MemoryAccess memoryAccess;

    /**
     * 执行动作
     *
     * @param context 执行上下文
     * @param action  动作
     * @param modelId 模型 ID
     * @return 执行结果
     */
    public String execute(ReActContext context, Action action, String modelId) {
        switch (action.getType()) {
            case TOOL -> {
                String toolName = action.getToolName();
                if (!context.isToolAllowed(toolName)) {
                    log.warn("[ActionExecutor] Tool {} not allowed for this character", toolName);
                    return "Tool not allowed: " + toolName;
                }
                try {
                    JsonNode paramsNode = new ObjectMapper().valueToTree(action.getParameters());
                    String toolCallId = UUID.randomUUID().toString();
                    ToolCallRequest toolCallRequest = new ToolCallRequest(toolCallId, toolName, paramsNode, modelId);
                    ToolUseContext toolUseContext = ToolUseContext.builder()
                            .sessionId(context.getExecutionId())
                            .agentId(context.getCharacterId())
                            .tenantId(context.getWorkspaceId())
                            .build();
                    ToolExecutionService.MessageUpdate update = toolExecutionService
                            .runToolUse(toolCallRequest, context.getCharacterId(), context.getWorkspaceId(),
                                    toolUseContext, null)
                            .get(60, java.util.concurrent.TimeUnit.SECONDS);
                    // 从 MessageUpdate 提取工具结果文本
                    if (update != null && update.getMessage() != null) {
                        Object content = update.getMessage().get("content");
                        if (content instanceof java.util.List<?> blocks && !blocks.isEmpty()) {
                            Object firstBlock = blocks.getFirst();
                            if (firstBlock instanceof org.dragon.tool.runtime.ToolResultBlockParam block) {
                                return block.getContent() != null ? block.getContent().toString() : "";
                            } else if (firstBlock instanceof Map<?, ?> blockMap) {
                                Object c = blockMap.get("content");
                                return c != null ? c.toString() : "";
                            }
                        }
                        Object toolUseResult = update.getMessage().get("toolUseResult");
                        return toolUseResult != null ? toolUseResult.toString() : "";
                    }
                    return "Tool execution returned no result";
                } catch (Exception e) {
                    log.error("[ActionExecutor] Tool execution failed: tool={}, error={}", action.getToolName(), e.getMessage(), e);
                    return "Tool execution failed: " + e.getMessage();
                }
            }

            case MEMORY -> {
                return memoryAccess.semanticSearch(
                        (String) action.getParameters().get("query"),
                        (Integer) action.getParameters().getOrDefault("topK", 5)
                ).toString();
            }

            case RESPOND, FINISH -> {
                return action.getResponse() != null ? action.getResponse() : action.getToolName();
            }

            default -> {
                return "Unknown action type";
            }
        }
    }
}

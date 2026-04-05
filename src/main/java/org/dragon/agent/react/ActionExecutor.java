package org.dragon.agent.react;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.dragon.character.mind.memory.MemoryAccess;
import org.dragon.tools.AgentTool;
import org.dragon.tools.ToolRegistry;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    private final ToolRegistry toolRegistry;
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
                Optional<AgentTool> toolOpt = toolRegistry.get(toolName);
                if (toolOpt.isPresent()) {
                    AgentTool tool = toolOpt.get();
                    JsonNode paramsNode = new ObjectMapper().valueToTree(action.getParameters());
                    AgentTool.ToolContext toolContext = AgentTool.ToolContext.builder()
                            .parameters(paramsNode)
                            .characterId(context.getCharacterId())
                            .workspaceId(context.getWorkspaceId())
                            .build();
                    try {
                        AgentTool.ToolResult result = tool.execute(toolContext)
                                .toCompletableFuture()
                                .get(30, TimeUnit.SECONDS);
                        return result.isSuccess() ? result.getOutput() : "Tool error: " + result.getError();
                    } catch (Exception e) {
                        return "Tool execution failed: " + e.getMessage();
                    }
                }
                return "Tool not found: " + toolName;
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

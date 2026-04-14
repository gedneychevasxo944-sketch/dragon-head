package org.dragon.tool.runtime.tools.character.observer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * ExploreObservationNeeds 工具
 * 根据用户请求和目标信息，分析需要调用哪些数据获取工具
 */
@Slf4j
@Component
public class ExploreObservationNeedsTool extends AbstractTool<JsonNode, String> {

    public ExploreObservationNeedsTool() {
        super("explore_observation_needs",
                "分析用户请求，确定需要调用哪些数据获取工具来收集观测数据",
                JsonNode.class);
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetType = input.has("targetType") ? input.get("targetType").asText() : null;
                String targetId = input.has("targetId") ? input.get("targetId").asText() : null;

                if (targetId == null || targetId.isEmpty()) {
                    return ToolResult.fail("Missing required parameter: targetId");
                }

                List<String> requiredTools = new ArrayList<>();
                Map<String, Object> toolParams = new HashMap<>();

                if ("CHARACTER".equalsIgnoreCase(targetType)) {
                    requiredTools.add("get_character_state");
                    toolParams.put("get_character_state", Map.of("characterId", targetId));
                    requiredTools.add("get_recent_tasks");
                    toolParams.put("get_recent_tasks", Map.of("targetType", "CHARACTER", "targetId", targetId, "days", 7));
                } else if ("WORKSPACE".equalsIgnoreCase(targetType)) {
                    requiredTools.add("get_workspace_state");
                    toolParams.put("get_workspace_state", Map.of("workspaceId", targetId));
                    requiredTools.add("get_recent_tasks");
                    toolParams.put("get_recent_tasks", Map.of("targetType", "WORKSPACE", "targetId", targetId, "days", 7));
                }

                requiredTools.add("get_evaluation_records");
                toolParams.put("get_evaluation_records",
                        Map.of("targetType", targetType, "targetId", targetId, "days", 30));

                Map<String, Object> result = new HashMap<>();
                result.put("requiredTools", requiredTools);
                result.put("params", toolParams);
                result.put("analysis", "Based on the request, I need to gather: " + String.join(", ", requiredTools));

                String json = objectMapper.writeValueAsString(result);
                return ToolResult.ok(json, ToolResultBlockParam.ofText(context.getToolUseId(), json));

            } catch (Exception e) {
                log.error("[ExploreObservationNeedsTool] Execution failed", e);
                return ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

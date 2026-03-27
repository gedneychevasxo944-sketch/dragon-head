package org.dragon.tools.character.observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ExploreObservationNeeds 工具
 * 根据用户请求和目标信息，分析需要调用哪些数据获取工具
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExploreObservationNeedsTool implements AgentTool {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "explore_observation_needs";
    }

    @Override
    public String getDescription() {
        return "分析用户请求，确定需要调用哪些数据获取工具来收集观测数据";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode userPromptParam = properties.putObject("userPrompt");
        userPromptParam.put("type", "string");
        userPromptParam.put("description", "用户的请求 prompt");

        ObjectNode targetTypeParam = properties.putObject("targetType");
        targetTypeParam.put("type", "string");
        targetTypeParam.put("description", "目标类型：CHARACTER 或 WORKSPACE");

        ObjectNode targetIdParam = properties.putObject("targetId");
        targetIdParam.put("type", "string");
        targetIdParam.put("description", "目标 ID");

        schema.putArray("required").add("targetType").add("targetId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String targetType = params.has("targetType") ? params.get("targetType").asText() : null;
                String targetId = params.has("targetId") ? params.get("targetId").asText() : null;

                if (targetId == null || targetId.isEmpty()) {
                    return AgentTool.ToolResult.fail("Missing required parameter: targetId");
                }

                // 根据目标类型决定需要哪些工具
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

                // 总是需要评估记录
                requiredTools.add("get_evaluation_records");
                toolParams.put("get_evaluation_records", Map.of("targetType", targetType, "targetId", targetId, "days", 30));

                Map<String, Object> result = new HashMap<>();
                result.put("requiredTools", requiredTools);
                result.put("params", toolParams);
                result.put("analysis", "Based on the request, I need to gather: " + String.join(", ", requiredTools));

                return AgentTool.ToolResult.ok(objectMapper.writeValueAsString(result), result);

            } catch (Exception e) {
                log.error("[ExploreObservationNeedsTool] Execution failed", e);
                return AgentTool.ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }
}

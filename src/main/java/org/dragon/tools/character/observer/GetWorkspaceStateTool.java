package org.dragon.tools.character.observer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.WorkspaceObservationSnapshot;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GetWorkspaceState 工具
 * 获取 Workspace 的当前状态
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetWorkspaceStateTool implements AgentTool {

    private final DataCollector dataCollector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_workspace_state";
    }

    @Override
    public String getDescription() {
        return "获取 Workspace 的当前状态，包括成员列表、任务统计、配置信息等";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "Workspace ID");

        schema.putArray("required").add("workspaceId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String workspaceId = params.has("workspaceId") ? params.get("workspaceId").asText() : null;

                if (workspaceId == null || workspaceId.isEmpty()) {
                    return AgentTool.ToolResult.fail("Missing required parameter: workspaceId");
                }

                WorkspaceObservationSnapshot snapshot = dataCollector.collectWorkspaceObservation(workspaceId);
                if (snapshot == null) {
                    return AgentTool.ToolResult.fail("Workspace not found: " + workspaceId);
                }

                return AgentTool.ToolResult.ok(objectMapper.writeValueAsString(snapshot), Map.of("snapshot", snapshot));

            } catch (Exception e) {
                log.error("[GetWorkspaceStateTool] Execution failed", e);
                return AgentTool.ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }
}

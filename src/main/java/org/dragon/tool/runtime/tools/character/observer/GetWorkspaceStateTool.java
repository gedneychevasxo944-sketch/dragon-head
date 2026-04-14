package org.dragon.tool.runtime.tools.character.observer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.WorkspaceObservationSnapshot;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * GetWorkspaceState 工具
 * 获取 Workspace 的当前状态
 */
@Slf4j
@Component
public class GetWorkspaceStateTool extends AbstractTool<JsonNode, String> {

    private final DataCollector dataCollector;

    @Autowired
    public GetWorkspaceStateTool(DataCollector dataCollector) {
        super("get_workspace_state",
                "获取 Workspace 的当前状态，包括成员列表、任务统计、配置信息等",
                JsonNode.class);
        this.dataCollector = dataCollector;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String workspaceId = input.has("workspaceId") ? input.get("workspaceId").asText() : null;

                if (workspaceId == null || workspaceId.isEmpty()) {
                    return ToolResult.fail("Missing required parameter: workspaceId");
                }

                WorkspaceObservationSnapshot snapshot = dataCollector.collectWorkspaceObservation(workspaceId);
                if (snapshot == null) {
                    return ToolResult.fail("Workspace not found: " + workspaceId);
                }

                String json = objectMapper.writeValueAsString(snapshot);
                return ToolResult.ok(json, ToolResultBlockParam.ofText(context.getToolUseId(), json));

            } catch (Exception e) {
                log.error("[GetWorkspaceStateTool] Execution failed", e);
                return ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

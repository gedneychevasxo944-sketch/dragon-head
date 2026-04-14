package org.dragon.tool.runtime.tools.character.observer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * GetRecentTasks 工具
 * 获取最近的任务数据
 */
@Slf4j
@Component
public class GetRecentTasksTool extends AbstractTool<JsonNode, String> {

    private final DataCollector dataCollector;

    @Autowired
    public GetRecentTasksTool(DataCollector dataCollector) {
        super("get_recent_tasks",
                "获取最近的任务执行数据，包括任务输入、输出、耗时、成功率等",
                JsonNode.class);
        this.dataCollector = dataCollector;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetType = input.has("targetType") ? input.get("targetType").asText() : null;
                String targetId = input.has("targetId") ? input.get("targetId").asText() : null;
                int days = input.has("days") ? input.get("days").asInt(7) : 7;

                if (targetId == null || targetId.isEmpty()) {
                    return ToolResult.fail("Missing required parameter: targetId");
                }

                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusDays(days);

                List<EvaluationEngine.TaskData> tasks;
                if ("CHARACTER".equalsIgnoreCase(targetType)) {
                    tasks = dataCollector.collectCharacterTaskData(targetId, startTime, endTime);
                } else if ("WORKSPACE".equalsIgnoreCase(targetType)) {
                    tasks = dataCollector.collectWorkspaceTaskData(targetId, startTime, endTime);
                } else {
                    return ToolResult.fail("Invalid targetType: " + targetType);
                }

                String json = objectMapper.writeValueAsString(tasks);
                return ToolResult.ok(json, ToolResultBlockParam.ofText(context.getToolUseId(), json));

            } catch (Exception e) {
                log.error("[GetRecentTasksTool] Execution failed", e);
                return ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

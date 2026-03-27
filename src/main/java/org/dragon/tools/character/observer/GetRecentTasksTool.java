package org.dragon.tools.character.observer;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GetRecentTasks 工具
 * 获取最近的任务数据
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetRecentTasksTool implements AgentTool {

    private final DataCollector dataCollector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_recent_tasks";
    }

    @Override
    public String getDescription() {
        return "获取最近的任务执行数据，包括任务输入、输出、耗时、成功率等";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode targetTypeParam = properties.putObject("targetType");
        targetTypeParam.put("type", "string");
        targetTypeParam.put("description", "目标类型：CHARACTER 或 WORKSPACE");

        ObjectNode targetIdParam = properties.putObject("targetId");
        targetIdParam.put("type", "string");
        targetIdParam.put("description", "目标 ID");

        ObjectNode daysParam = properties.putObject("days");
        daysParam.put("type", "integer");
        daysParam.put("description", "收集最近多少天的数据，默认 7 天");
        daysParam.put("default", 7);

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
                int days = params.has("days") ? params.get("days").asInt(7) : 7;

                if (targetId == null || targetId.isEmpty()) {
                    return AgentTool.ToolResult.fail("Missing required parameter: targetId");
                }

                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusDays(days);

                java.util.List<EvaluationEngine.TaskData> tasks;
                if ("CHARACTER".equalsIgnoreCase(targetType)) {
                    tasks = dataCollector.collectCharacterTaskData(targetId, startTime, endTime);
                } else if ("WORKSPACE".equalsIgnoreCase(targetType)) {
                    tasks = dataCollector.collectWorkspaceTaskData(targetId, startTime, endTime);
                } else {
                    return AgentTool.ToolResult.fail("Invalid targetType: " + targetType);
                }

                return AgentTool.ToolResult.ok(
                        objectMapper.writeValueAsString(tasks),
                        Map.of(
                                "tasks", tasks,
                                "count", tasks.size(),
                                "timeRange", Map.of("start", startTime.toString(), "end", endTime.toString())
                        )
                );

            } catch (Exception e) {
                log.error("[GetRecentTasksTool] Execution failed", e);
                return AgentTool.ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }
}

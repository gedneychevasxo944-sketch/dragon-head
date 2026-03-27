package org.dragon.tools.character.observer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GetEvaluationRecords 工具
 * 获取历史评估记录
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetEvaluationRecordsTool implements AgentTool {

    private final EvaluationRecordStore evaluationRecordStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_evaluation_records";
    }

    @Override
    public String getDescription() {
        return "获取历史评估记录，包括评分、发现的问题、改进建议等";
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
        daysParam.put("description", "收集最近多少天的数据，默认 30 天");
        daysParam.put("default", 30);

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
                int days = params.has("days") ? params.get("days").asInt(30) : 30;

                if (targetId == null || targetId.isEmpty()) {
                    return AgentTool.ToolResult.fail("Missing required parameter: targetId");
                }

                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusDays(days);

                EvaluationRecord.TargetType evalTargetType = "CHARACTER".equalsIgnoreCase(targetType)
                        ? EvaluationRecord.TargetType.CHARACTER
                        : EvaluationRecord.TargetType.WORKSPACE;

                List<EvaluationRecord> records = evaluationRecordStore.findByTargetAndTimeRange(
                        evalTargetType, targetId, startTime, endTime);

                return AgentTool.ToolResult.ok(
                        objectMapper.writeValueAsString(records),
                        Map.of("records", records, "count", records.size())
                );

            } catch (Exception e) {
                log.error("[GetEvaluationRecordsTool] Execution failed", e);
                return AgentTool.ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }
}

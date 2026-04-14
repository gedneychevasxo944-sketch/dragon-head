package org.dragon.tool.runtime.tools.character.observer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.dragon.store.StoreFactory;
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
 * GetEvaluationRecords 工具
 * 获取历史评估记录
 */
@Slf4j
@Component
public class GetEvaluationRecordsTool extends AbstractTool<JsonNode, String> {

    private final StoreFactory storeFactory;

    @Autowired
    public GetEvaluationRecordsTool(StoreFactory storeFactory) {
        super("get_evaluation_records",
                "获取历史评估记录，包括评分、发现的问题、改进建议等",
                JsonNode.class);
        this.storeFactory = storeFactory;
    }

    private EvaluationRecordStore getEvaluationRecordStore() {
        return storeFactory.get(EvaluationRecordStore.class);
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String targetType = input.has("targetType") ? input.get("targetType").asText() : null;
                String targetId = input.has("targetId") ? input.get("targetId").asText() : null;
                int days = input.has("days") ? input.get("days").asInt(30) : 30;

                if (targetId == null || targetId.isEmpty()) {
                    return ToolResult.fail("Missing required parameter: targetId");
                }

                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusDays(days);

                EvaluationRecord.TargetType evalTargetType = "CHARACTER".equalsIgnoreCase(targetType)
                        ? EvaluationRecord.TargetType.CHARACTER
                        : EvaluationRecord.TargetType.WORKSPACE;

                List<EvaluationRecord> records = getEvaluationRecordStore()
                        .findByTargetAndTimeRange(evalTargetType, targetId, startTime, endTime);

                String json = objectMapper.writeValueAsString(records);
                return ToolResult.ok(json, ToolResultBlockParam.ofText(context.getToolUseId(), json));

            } catch (Exception e) {
                log.error("[GetEvaluationRecordsTool] Execution failed", e);
                return ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

package org.dragon.tools.character.observer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.CharacterObservationSnapshot;
import org.dragon.observer.collector.dto.WorkspaceObservationSnapshot;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.dragon.store.StoreFactory;
import org.dragon.tools.AgentTool;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ObserverAdvisor Character 工具类
 * 提供数据获取相关的工具，供 ObserverAdvisor Character 使用
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ObserverAdvisorCharacterTools {

    private static final String TOOL_EXPLORE_NEEDS = "explore_observation_needs";
    private static final String TOOL_GET_CHARACTER_STATE = "get_character_state";
    private static final String TOOL_GET_WORKSPACE_STATE = "get_workspace_state";
    private static final String TOOL_GET_RECENT_TASKS = "get_recent_tasks";
    private static final String TOOL_GET_EVALUATION_RECORDS = "get_evaluation_records";

    private final DataCollector dataCollector;
    private final StoreFactory storeFactory;
    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private EvaluationRecordStore getEvaluationRecordStore() {
        return storeFactory.get(EvaluationRecordStore.class);
    }

    /**
     * 获取所有可用工具
     */
    public List<AgentTool> getAvailableTools() {
        List<AgentTool> tools = new ArrayList<>();
        tools.add(new ExploreObservationNeedsTool());
        tools.add(new GetCharacterStateTool());
        tools.add(new GetWorkspaceStateTool());
        tools.add(new GetRecentTasksTool());
        tools.add(new GetEvaluationRecordsTool());
        return tools;
    }

    /**
     * 获取指定名称的工具
     */
    public AgentTool getTool(String name) {
        return switch (name) {
            case TOOL_EXPLORE_NEEDS -> new ExploreObservationNeedsTool();
            case TOOL_GET_CHARACTER_STATE -> new GetCharacterStateTool();
            case TOOL_GET_WORKSPACE_STATE -> new GetWorkspaceStateTool();
            case TOOL_GET_RECENT_TASKS -> new GetRecentTasksTool();
            case TOOL_GET_EVALUATION_RECORDS -> new GetEvaluationRecordsTool();
            default -> null;
        };
    }

    private Map<String, Object> toMap(JsonNode node) {
        return objectMapper.convertValue(node, Map.class);
    }

    /**
     * ExploreObservationNeeds 工具
     * 根据用户请求和目标信息，分析需要调用哪些数据获取工具
     */
    private class ExploreObservationNeedsTool implements AgentTool {

        @Override
        public String getName() {
            return TOOL_EXPLORE_NEEDS;
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

            ObjectNode userPrompt = properties.putObject("userPrompt");
            userPrompt.put("type", "string");
            userPrompt.put("description", "用户的请求 prompt");

            ObjectNode targetType = properties.putObject("targetType");
            targetType.put("type", "string");
            targetType.put("description", "目标类型：CHARACTER 或 WORKSPACE");

            ObjectNode targetId = properties.putObject("targetId");
            targetId.put("type", "string");
            targetId.put("description", "目标 ID");

            schema.putArray("required").add("targetType").add("targetId");
            return schema;
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolContext context) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> params = toMap(context.getParameters());
                    String targetType = (String) params.get("targetType");
                    String targetId = (String) params.get("targetId");

                    if (targetId == null || targetId.isEmpty()) {
                        return ToolResult.fail("Missing required parameter: targetId");
                    }

                    // 根据目标类型决定需要哪些工具
                    List<String> requiredTools = new ArrayList<>();
                    Map<String, Object> toolParams = new HashMap<>();

                    if ("CHARACTER".equalsIgnoreCase(targetType)) {
                        requiredTools.add(TOOL_GET_CHARACTER_STATE);
                        toolParams.put(TOOL_GET_CHARACTER_STATE, Map.<String, Object>of("characterId", targetId));
                        requiredTools.add(TOOL_GET_RECENT_TASKS);
                        toolParams.put(TOOL_GET_RECENT_TASKS, Map.<String, Object>of("targetType", "CHARACTER", "targetId", targetId, "days", 7));
                    } else if ("WORKSPACE".equalsIgnoreCase(targetType)) {
                        requiredTools.add(TOOL_GET_WORKSPACE_STATE);
                        toolParams.put(TOOL_GET_WORKSPACE_STATE, Map.<String, Object>of("workspaceId", targetId));
                        requiredTools.add(TOOL_GET_RECENT_TASKS);
                        toolParams.put(TOOL_GET_RECENT_TASKS, Map.<String, Object>of("targetType", "WORKSPACE", "targetId", targetId, "days", 7));
                    }

                    // 总是需要评估记录
                    requiredTools.add(TOOL_GET_EVALUATION_RECORDS);
                    toolParams.put(TOOL_GET_EVALUATION_RECORDS, Map.<String, Object>of("targetType", targetType, "targetId", targetId, "days", 30));

                    Map<String, Object> result = new HashMap<>();
                    result.put("requiredTools", requiredTools);
                    result.put("params", toolParams);
                    result.put("analysis", "Based on the request, I need to gather: " + String.join(", ", requiredTools));

                    return ToolResult.ok(gson.toJson(result), result);

                } catch (Exception e) {
                    log.error("[ExploreObservationNeedsTool] Execution failed", e);
                    return ToolResult.fail("Error: " + e.getMessage());
                }
            });
        }
    }

    /**
     * GetCharacterState 工具
     * 获取 Character 的当前状态
     */
    private class GetCharacterStateTool implements AgentTool {

        @Override
        public String getName() {
            return TOOL_GET_CHARACTER_STATE;
        }

        @Override
        public String getDescription() {
            return "获取 Character 的当前状态，包括基本信息、任务统计、技能标签等";
        }

        @Override
        public JsonNode getParameterSchema() {
            ObjectNode schema = objectMapper.createObjectNode();
            schema.put("type", "object");
            ObjectNode properties = schema.putObject("properties");

            ObjectNode characterId = properties.putObject("characterId");
            characterId.put("type", "string");
            characterId.put("description", "Character ID");

            schema.putArray("required").add("characterId");
            return schema;
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolContext context) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> params = toMap(context.getParameters());
                    String characterId = (String) params.get("characterId");

                    if (characterId == null || characterId.isEmpty()) {
                        return ToolResult.fail("Missing required parameter: characterId");
                    }

                    CharacterObservationSnapshot snapshot = dataCollector.collectCharacterObservation(characterId);
                    if (snapshot == null) {
                        return ToolResult.fail("Character not found: " + characterId);
                    }

                    Map<String, Object> data = Map.of("snapshot", snapshot);
                    return ToolResult.ok(gson.toJson(snapshot), data);

                } catch (Exception e) {
                    log.error("[GetCharacterStateTool] Execution failed", e);
                    return ToolResult.fail("Error: " + e.getMessage());
                }
            });
        }
    }

    /**
     * GetWorkspaceState 工具
     * 获取 Workspace 的当前状态
     */
    private class GetWorkspaceStateTool implements AgentTool {

        @Override
        public String getName() {
            return TOOL_GET_WORKSPACE_STATE;
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

            ObjectNode workspaceId = properties.putObject("workspaceId");
            workspaceId.put("type", "string");
            workspaceId.put("description", "Workspace ID");

            schema.putArray("required").add("workspaceId");
            return schema;
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolContext context) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> params = toMap(context.getParameters());
                    String workspaceId = (String) params.get("workspaceId");

                    if (workspaceId == null || workspaceId.isEmpty()) {
                        return ToolResult.fail("Missing required parameter: workspaceId");
                    }

                    WorkspaceObservationSnapshot snapshot = dataCollector.collectWorkspaceObservation(workspaceId);
                    if (snapshot == null) {
                        return ToolResult.fail("Workspace not found: " + workspaceId);
                    }

                    Map<String, Object> data = Map.of("snapshot", snapshot);
                    return ToolResult.ok(gson.toJson(snapshot), data);

                } catch (Exception e) {
                    log.error("[GetWorkspaceStateTool] Execution failed", e);
                    return ToolResult.fail("Error: " + e.getMessage());
                }
            });
        }
    }

    /**
     * GetRecentTasks 工具
     * 获取最近的任务数据
     */
    private class GetRecentTasksTool implements AgentTool {

        @Override
        public String getName() {
            return TOOL_GET_RECENT_TASKS;
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

            ObjectNode targetType = properties.putObject("targetType");
            targetType.put("type", "string");
            targetType.put("description", "目标类型：CHARACTER 或 WORKSPACE");

            ObjectNode targetId = properties.putObject("targetId");
            targetId.put("type", "string");
            targetId.put("description", "目标 ID");

            ObjectNode days = properties.putObject("days");
            days.put("type", "integer");
            days.put("description", "收集最近多少天的数据，默认 7 天");
            days.put("default", 7);

            schema.putArray("required").add("targetType").add("targetId");
            return schema;
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolContext context) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> params = toMap(context.getParameters());
                    String targetType = (String) params.get("targetType");
                    String targetId = (String) params.get("targetId");
                    int days = params.containsKey("days") ? ((Number) params.get("days")).intValue() : 7;

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

                    Map<String, Object> data = Map.of(
                            "tasks", tasks,
                            "count", tasks.size(),
                            "timeRange", Map.of("start", startTime.toString(), "end", endTime.toString())
                    );
                    return ToolResult.ok(gson.toJson(tasks), data);

                } catch (Exception e) {
                    log.error("[GetRecentTasksTool] Execution failed", e);
                    return ToolResult.fail("Error: " + e.getMessage());
                }
            });
        }
    }

    /**
     * GetEvaluationRecords 工具
     * 获取历史评估记录
     */
    private class GetEvaluationRecordsTool implements AgentTool {

        @Override
        public String getName() {
            return TOOL_GET_EVALUATION_RECORDS;
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

            ObjectNode targetType = properties.putObject("targetType");
            targetType.put("type", "string");
            targetType.put("description", "目标类型：CHARACTER 或 WORKSPACE");

            ObjectNode targetId = properties.putObject("targetId");
            targetId.put("type", "string");
            targetId.put("description", "目标 ID");

            ObjectNode days = properties.putObject("days");
            days.put("type", "integer");
            days.put("description", "收集最近多少天的数据，默认 30 天");
            days.put("default", 30);

            schema.putArray("required").add("targetType").add("targetId");
            return schema;
        }

        @Override
        public CompletableFuture<ToolResult> execute(ToolContext context) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Map<String, Object> params = toMap(context.getParameters());
                    String targetType = (String) params.get("targetType");
                    String targetId = (String) params.get("targetId");
                    int days = params.containsKey("days") ? ((Number) params.get("days")).intValue() : 30;

                    if (targetId == null || targetId.isEmpty()) {
                        return ToolResult.fail("Missing required parameter: targetId");
                    }

                    LocalDateTime endTime = LocalDateTime.now();
                    LocalDateTime startTime = endTime.minusDays(days);

                    EvaluationRecord.TargetType evalTargetType = "CHARACTER".equalsIgnoreCase(targetType)
                            ? EvaluationRecord.TargetType.CHARACTER
                            : EvaluationRecord.TargetType.WORKSPACE;

                    List<EvaluationRecord> records = getEvaluationRecordStore().findByTargetAndTimeRange(
                            evalTargetType, targetId, startTime, endTime);

                    Map<String, Object> data = Map.of("records", records, "count", records.size());
                    return ToolResult.ok(gson.toJson(records), data);

                } catch (Exception e) {
                    log.error("[GetEvaluationRecordsTool] Execution failed", e);
                    return ToolResult.fail("Error: " + e.getMessage());
                }
            });
        }
    }
}
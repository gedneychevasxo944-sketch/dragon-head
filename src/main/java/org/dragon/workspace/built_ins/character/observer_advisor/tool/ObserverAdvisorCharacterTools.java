package org.dragon.workspace.built_ins.character.observer_advisor.tool;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.CharacterObservationSnapshot;
import org.dragon.observer.collector.dto.WorkspaceObservationSnapshot;
import org.dragon.observer.evaluation.EvaluationEngine;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.springframework.stereotype.Component;

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
    private final EvaluationRecordStore evaluationRecordStore;
    private final Gson gson = new Gson();

    /**
     * 获取所有可用工具
     */
    public List<ToolConnector> getAvailableTools() {
        List<ToolConnector> tools = new ArrayList<>();
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
    public ToolConnector getTool(String name) {
        return switch (name) {
            case TOOL_EXPLORE_NEEDS -> new ExploreObservationNeedsTool();
            case TOOL_GET_CHARACTER_STATE -> new GetCharacterStateTool();
            case TOOL_GET_WORKSPACE_STATE -> new GetWorkspaceStateTool();
            case TOOL_GET_RECENT_TASKS -> new GetRecentTasksTool();
            case TOOL_GET_EVALUATION_RECORDS -> new GetEvaluationRecordsTool();
            default -> null;
        };
    }

    /**
     * ExploreObservationNeeds 工具
     * 根据用户请求和目标信息，分析需要调用哪些数据获取工具
     */
    private class ExploreObservationNeedsTool implements ToolConnector {
        @Override
        public String getName() {
            return TOOL_EXPLORE_NEEDS;
        }

        @Override
        public ToolResult execute(Map<String, Object> params) {
            try {
                String targetType = (String) params.get("targetType");
                String targetId = (String) params.get("targetId");

                if (targetId == null || targetId.isEmpty()) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Missing required parameter: targetId")
                            .build();
                }

                // 根据目标类型决定需要哪些工具
                List<String> requiredTools = new ArrayList<>();
                Map<String, Object> toolParams = new HashMap<>();

                if ("CHARACTER".equalsIgnoreCase(targetType)) {
                    requiredTools.add(TOOL_GET_CHARACTER_STATE);
                    toolParams.put(TOOL_GET_CHARACTER_STATE, Map.of("characterId", targetId));
                    requiredTools.add(TOOL_GET_RECENT_TASKS);
                    toolParams.put(TOOL_GET_RECENT_TASKS, Map.of("targetType", "CHARACTER", "targetId", targetId, "days", 7));
                } else if ("WORKSPACE".equalsIgnoreCase(targetType)) {
                    requiredTools.add(TOOL_GET_WORKSPACE_STATE);
                    toolParams.put(TOOL_GET_WORKSPACE_STATE, Map.of("workspaceId", targetId));
                    requiredTools.add(TOOL_GET_RECENT_TASKS);
                    toolParams.put(TOOL_GET_RECENT_TASKS, Map.of("targetType", "WORKSPACE", "targetId", targetId, "days", 7));
                }

                // 总是需要评估记录
                requiredTools.add(TOOL_GET_EVALUATION_RECORDS);
                toolParams.put(TOOL_GET_EVALUATION_RECORDS, Map.of("targetType", targetType, "targetId", targetId, "days", 30));

                Map<String, Object> result = new HashMap<>();
                result.put("requiredTools", requiredTools);
                result.put("params", toolParams);
                result.put("analysis", "Based on the request, I need to gather: " + String.join(", ", requiredTools));

                return ToolResult.builder()
                        .success(true)
                        .content(gson.toJson(result))
                        .data(result)
                        .build();

            } catch (Exception e) {
                log.error("[ExploreObservationNeedsTool] Execution failed", e);
                return ToolResult.builder()
                        .success(false)
                        .content("Error: " + e.getMessage())
                        .build();
            }
        }

        @Override
        public ToolSchema getSchema() {
            return ToolSchema.builder()
                    .name(getName())
                    .description("分析用户请求，确定需要调用哪些数据获取工具来收集观测数据")
                    .inputParameters(List.of(
                            ToolParameter.builder()
                                    .name("userPrompt")
                                    .type("string")
                                    .description("用户的请求 prompt")
                                    .required(false)
                                    .build(),
                            ToolParameter.builder()
                                    .name("targetType")
                                    .type("string")
                                    .description("目标类型：CHARACTER 或 WORKSPACE")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("targetId")
                                    .type("string")
                                    .description("目标 ID")
                                    .required(true)
                                    .build()
                    ))
                    .build();
        }
    }

    /**
     * GetCharacterState 工具
     * 获取 Character 的当前状态
     */
    private class GetCharacterStateTool implements ToolConnector {
        @Override
        public String getName() {
            return TOOL_GET_CHARACTER_STATE;
        }

        @Override
        public ToolResult execute(Map<String, Object> params) {
            try {
                String characterId = (String) params.get("characterId");

                if (characterId == null || characterId.isEmpty()) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Missing required parameter: characterId")
                            .build();
                }

                CharacterObservationSnapshot snapshot = dataCollector.collectCharacterObservation(characterId);
                if (snapshot == null) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Character not found: " + characterId)
                            .build();
                }

                return ToolResult.builder()
                        .success(true)
                        .content(gson.toJson(snapshot))
                        .data(Map.of("snapshot", snapshot))
                        .build();

            } catch (Exception e) {
                log.error("[GetCharacterStateTool] Execution failed", e);
                return ToolResult.builder()
                        .success(false)
                        .content("Error: " + e.getMessage())
                        .build();
            }
        }

        @Override
        public ToolSchema getSchema() {
            return ToolSchema.builder()
                    .name(getName())
                    .description("获取 Character 的当前状态，包括基本信息、任务统计、技能标签等")
                    .inputParameters(List.of(
                            ToolParameter.builder()
                                    .name("characterId")
                                    .type("string")
                                    .description("Character ID")
                                    .required(true)
                                    .build()
                    ))
                    .build();
        }
    }

    /**
     * GetWorkspaceState 工具
     * 获取 Workspace 的当前状态
     */
    private class GetWorkspaceStateTool implements ToolConnector {
        @Override
        public String getName() {
            return TOOL_GET_WORKSPACE_STATE;
        }

        @Override
        public ToolResult execute(Map<String, Object> params) {
            try {
                String workspaceId = (String) params.get("workspaceId");

                if (workspaceId == null || workspaceId.isEmpty()) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Missing required parameter: workspaceId")
                            .build();
                }

                WorkspaceObservationSnapshot snapshot = dataCollector.collectWorkspaceObservation(workspaceId);
                if (snapshot == null) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Workspace not found: " + workspaceId)
                            .build();
                }

                return ToolResult.builder()
                        .success(true)
                        .content(gson.toJson(snapshot))
                        .data(Map.of("snapshot", snapshot))
                        .build();

            } catch (Exception e) {
                log.error("[GetWorkspaceStateTool] Execution failed", e);
                return ToolResult.builder()
                        .success(false)
                        .content("Error: " + e.getMessage())
                        .build();
            }
        }

        @Override
        public ToolSchema getSchema() {
            return ToolSchema.builder()
                    .name(getName())
                    .description("获取 Workspace 的当前状态，包括成员列表、任务统计、配置信息等")
                    .inputParameters(List.of(
                            ToolParameter.builder()
                                    .name("workspaceId")
                                    .type("string")
                                    .description("Workspace ID")
                                    .required(true)
                                    .build()
                    ))
                    .build();
        }
    }

    /**
     * GetRecentTasks 工具
     * 获取最近的任务数据
     */
    private class GetRecentTasksTool implements ToolConnector {
        @Override
        public String getName() {
            return TOOL_GET_RECENT_TASKS;
        }

        @Override
        public ToolResult execute(Map<String, Object> params) {
            try {
                String targetType = (String) params.get("targetType");
                String targetId = (String) params.get("targetId");
                int days = params.containsKey("days") ? ((Number) params.get("days")).intValue() : 7;

                if (targetId == null || targetId.isEmpty()) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Missing required parameter: targetId")
                            .build();
                }

                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusDays(days);

                List<EvaluationEngine.TaskData> tasks;
                if ("CHARACTER".equalsIgnoreCase(targetType)) {
                    tasks = dataCollector.collectCharacterTaskData(targetId, startTime, endTime);
                } else if ("WORKSPACE".equalsIgnoreCase(targetType)) {
                    tasks = dataCollector.collectWorkspaceTaskData(targetId, startTime, endTime);
                } else {
                    return ToolResult.builder()
                            .success(false)
                            .content("Invalid targetType: " + targetType)
                            .build();
                }

                return ToolResult.builder()
                        .success(true)
                        .content(gson.toJson(tasks))
                        .data(Map.of("tasks", tasks, "count", tasks.size(), "timeRange", Map.of("start", startTime.toString(), "end", endTime.toString())))
                        .build();

            } catch (Exception e) {
                log.error("[GetRecentTasksTool] Execution failed", e);
                return ToolResult.builder()
                        .success(false)
                        .content("Error: " + e.getMessage())
                        .build();
            }
        }

        @Override
        public ToolSchema getSchema() {
            return ToolSchema.builder()
                    .name(getName())
                    .description("获取最近的任务执行数据，包括任务输入、输出、耗时、成功率等")
                    .inputParameters(List.of(
                            ToolParameter.builder()
                                    .name("targetType")
                                    .type("string")
                                    .description("目标类型：CHARACTER 或 WORKSPACE")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("targetId")
                                    .type("string")
                                    .description("目标 ID")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("days")
                                    .type("integer")
                                    .description("收集最近多少天的数据，默认 7 天")
                                    .required(false)
                                    .defaultValue(7)
                                    .build()
                    ))
                    .build();
        }
    }

    /**
     * GetEvaluationRecords 工具
     * 获取历史评估记录
     */
    private class GetEvaluationRecordsTool implements ToolConnector {
        @Override
        public String getName() {
            return TOOL_GET_EVALUATION_RECORDS;
        }

        @Override
        public ToolResult execute(Map<String, Object> params) {
            try {
                String targetType = (String) params.get("targetType");
                String targetId = (String) params.get("targetId");
                int days = params.containsKey("days") ? ((Number) params.get("days")).intValue() : 30;

                if (targetId == null || targetId.isEmpty()) {
                    return ToolResult.builder()
                            .success(false)
                            .content("Missing required parameter: targetId")
                            .build();
                }

                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusDays(days);

                EvaluationRecord.TargetType evalTargetType = "CHARACTER".equalsIgnoreCase(targetType)
                        ? EvaluationRecord.TargetType.CHARACTER
                        : EvaluationRecord.TargetType.WORKSPACE;

                List<EvaluationRecord> records = evaluationRecordStore.findByTargetAndTimeRange(
                        evalTargetType, targetId, startTime, endTime);

                return ToolResult.builder()
                        .success(true)
                        .content(gson.toJson(records))
                        .data(Map.of("records", records, "count", records.size()))
                        .build();

            } catch (Exception e) {
                log.error("[GetEvaluationRecordsTool] Execution failed", e);
                return ToolResult.builder()
                        .success(false)
                        .content("Error: " + e.getMessage())
                        .build();
            }
        }

        @Override
        public ToolSchema getSchema() {
            return ToolSchema.builder()
                    .name(getName())
                    .description("获取历史评估记录，包括评分、发现的问题、改进建议等")
                    .inputParameters(List.of(
                            ToolParameter.builder()
                                    .name("targetType")
                                    .type("string")
                                    .description("目标类型：CHARACTER 或 WORKSPACE")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("targetId")
                                    .type("string")
                                    .description("目标 ID")
                                    .required(true)
                                    .build(),
                            ToolParameter.builder()
                                    .name("days")
                                    .type("integer")
                                    .description("收集最近多少天的数据，默认 30 天")
                                    .required(false)
                                    .defaultValue(30)
                                    .build()
                    ))
                    .build();
        }
    }
}
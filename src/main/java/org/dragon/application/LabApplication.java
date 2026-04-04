package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LabApplication Lab 实验室模块应用服务
 *
 * <p>对应前端 /lab 页面，聚合场景插件、任务网络、拓扑图、行为演练场、技能组合器、时间线回放等业务逻辑。
 * 部分功能为占位实现（当前系统中对应数据结构较少），待后续演进。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabApplication {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final StoreFactory storeFactory;

    /** 场景内存存储：sceneId -> 场景数据 */
    private final Map<String, Map<String, Object>> sceneStore = new ConcurrentHashMap<>();
    /** 演练场景存储：scenarioId -> 演练配置 */
    private final Map<String, Map<String, Object>> arenaScenarioStore = new ConcurrentHashMap<>();
    /** 演练运行记录存储：runId -> 运行数据 */
    private final Map<String, Map<String, Object>> arenaRunStore = new ConcurrentHashMap<>();
    /** 技能图存储：graphId -> 技能图 */
    private final Map<String, Map<String, Object>> skillGraphStore = new ConcurrentHashMap<>();

    private TaskStore getTaskStore() {
        return storeFactory.get(TaskStore.class);
    }

    // ==================== 33. Scene Plugin（场景插件）====================

    /**
     * 获取场景列表。
     *
     * @return 场景列表
     */
    public List<Map<String, Object>> listScenes() {
        log.info("[LabApplication] listScenes");
        return new ArrayList<>(sceneStore.values());
    }

    /**
     * 创建场景。
     *
     * @param sceneData 场景数据
     * @return 创建后的场景
     */
    public Map<String, Object> createScene(Map<String, Object> sceneData) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> scene = new HashMap<>(sceneData);
        scene.put("id", id);
        scene.put("status", scene.getOrDefault("status", "draft"));
        scene.put("instanceCount", 0);
        scene.put("createdAt", LocalDateTime.now().toString());
        scene.put("updatedAt", LocalDateTime.now().toString());
        sceneStore.put(id, scene);
        log.info("[LabApplication] createScene id={}", id);
        return scene;
    }

    /**
     * 更新场景。
     *
     * @param id        场景 ID
     * @param sceneData 更新数据
     * @return 更新后的场景
     */
    public Map<String, Object> updateScene(String id, Map<String, Object> sceneData) {
        Map<String, Object> existing = sceneStore.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Scene not found: " + id);
        }
        existing.putAll(sceneData);
        existing.put("id", id);
        existing.put("updatedAt", LocalDateTime.now().toString());
        sceneStore.put(id, existing);
        log.info("[LabApplication] updateScene id={}", id);
        return existing;
    }

    /**
     * 获取场景实例列表（占位）。
     *
     * @param sceneId 场景 ID
     * @return 实例列表
     */
    public List<Map<String, Object>> listSceneInstances(String sceneId) {
        log.info("[LabApplication] listSceneInstances sceneId={}", sceneId);
        return List.of();
    }

    // ==================== 34. Task Network（任务网络）====================

    /**
     * 分页获取任务列表（全局任务网络视图）。
     *
     * @param status   状态筛选
     * @param priority 优先级筛选
     * @return 任务列表
     */
    public List<Map<String, Object>> listLabTasks(String status, String priority) {
        log.info("[LabApplication] listLabTasks status={} priority={}", status, priority);

        List<Task> tasks;
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                TaskStatus ts = TaskStatus.valueOf(status.toUpperCase());
                tasks = getTaskStore().findByStatus(ts);
            } catch (IllegalArgumentException e) {
                tasks = getAllTasks();
            }
        } else {
            tasks = getAllTasks();
        }

        return tasks.stream()
                .map(this::toLabTaskMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    public Map<String, Object> getLabTask(String taskId) {
        Task task = getTaskStore().findById(taskId).orElse(null);
        if (task == null) {
            return null;
        }
        return toLabTaskMap(task);
    }

    /**
     * 更新任务状态。
     *
     * @param taskId    任务 ID
     * @param newStatus 新状态
     * @return 更新后的任务
     */
    public Map<String, Object> updateLabTaskStatus(String taskId, String newStatus) {
        Task task = getTaskStore().findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        try {
            TaskStatus ts = TaskStatus.valueOf(newStatus.toUpperCase());
            task.setStatus(ts);
            task.setUpdatedAt(LocalDateTime.now());
            getTaskStore().update(task);
            log.info("[LabApplication] updateLabTaskStatus taskId={} status={}", taskId, newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid task status: " + newStatus);
        }

        return toLabTaskMap(task);
    }

    /**
     * 提交任务验收结果（占位）。
     *
     * @param taskId   任务 ID
     * @param approval 验收数据
     * @return 验收结果
     */
    public Map<String, Object> submitTaskAcceptance(String taskId, Map<String, Object> approval) {
        log.info("[LabApplication] submitTaskAcceptance taskId={}", taskId);
        Task task = getTaskStore().findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        Boolean approved = (Boolean) approval.get("approved");
        if (Boolean.TRUE.equals(approved)) {
            task.setStatus(TaskStatus.COMPLETED);
        } else {
            task.setStatus(TaskStatus.FAILED);
        }
        task.setUpdatedAt(LocalDateTime.now());
        getTaskStore().update(task);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("accepted", approved);
        result.put("acceptedAt", LocalDateTime.now().toString());
        return result;
    }

    // ==================== 35. Topology（拓扑图）====================

    /**
     * 获取系统拓扑图数据。
     *
     * @param nodeTypes   节点类型过滤（逗号分隔）
     * @param edgeTypes   边类型过滤
     * @param workspaceId 特定 Workspace
     * @return 拓扑图（含 nodes 和 edges）
     */
    public Map<String, Object> getTopology(String nodeTypes, String edgeTypes, String workspaceId) {
        log.info("[LabApplication] getTopology nodeTypes={} workspaceId={}", nodeTypes, workspaceId);

        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        boolean includeWorkspace = nodeTypes == null || nodeTypes.contains("workspace");
        boolean includeCharacter = nodeTypes == null || nodeTypes.contains("character");

        // 构建 Workspace 节点
        List<Workspace> workspaces = workspaceId != null && !workspaceId.isBlank()
                ? workspaceRegistry.get(workspaceId).map(List::of).orElse(List.of())
                : workspaceRegistry.listAll();

        if (includeWorkspace) {
            for (Workspace ws : workspaces) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", ws.getId());
                node.put("type", "workspace");
                node.put("name", ws.getName() != null ? ws.getName() : "");
                node.put("description", ws.getDescription() != null ? ws.getDescription() : "");
                node.put("size", "large");
                node.put("status", ws.getStatus() != null
                        ? ws.getStatus().name().toLowerCase() : "inactive");
                node.put("connectionCount", 0);
                nodes.add(node);
            }
        }

        // 构建 Character 节点及 Workspace-Character 边
        if (includeCharacter) {
            List<Character> characters = characterRegistry.listAll();
            for (Character c : characters) {
                Map<String, Object> node = new HashMap<>();
                node.put("id", c.getId());
                node.put("type", "character");
                node.put("name", c.getName() != null ? c.getName() : "");
                node.put("description", c.getDescription() != null ? c.getDescription() : "");
                node.put("size", "medium");
                node.put("status", c.getStatus() != null
                        ? c.getStatus().name().toLowerCase() : "inactive");
                node.put("connectionCount", 0);
                nodes.add(node);

                // 构建 Workspace -> Character 边
                List<String> wsIds = c.getWorkspaceIds();
                if (wsIds != null) {
                    for (String wsId : wsIds) {
                        if (workspaceId == null || workspaceId.isBlank() || workspaceId.equals(wsId)) {
                            Map<String, Object> edge = new HashMap<>();
                            edge.put("id", wsId + "_owns_" + c.getId());
                            edge.put("source", wsId);
                            edge.put("target", c.getId());
                            edge.put("type", "owns");
                            edge.put("weight", 1.0);
                            edge.put("label", "owns");
                            edges.add(edge);
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    // ==================== 36. Behavior Arena（行为演练场）====================

    /**
     * 获取演练场景列表。
     *
     * @return 演练场景列表
     */
    public List<Map<String, Object>> listArenaScenarios() {
        log.info("[LabApplication] listArenaScenarios");
        return new ArrayList<>(arenaScenarioStore.values());
    }

    /**
     * 创建演练场景。
     *
     * @param scenarioData 场景配置
     * @return 创建后的场景
     */
    public Map<String, Object> createArenaScenario(Map<String, Object> scenarioData) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> scenario = new HashMap<>(scenarioData);
        scenario.put("id", id);
        scenario.put("createdAt", LocalDateTime.now().toString());
        scenario.put("updatedAt", LocalDateTime.now().toString());
        arenaScenarioStore.put(id, scenario);
        log.info("[LabApplication] createArenaScenario id={}", id);
        return scenario;
    }

    /**
     * 运行演练场景。
     *
     * @param scenarioId 场景 ID
     * @return 运行记录（SimulationRun）
     */
    public Map<String, Object> runArenaScenario(String scenarioId) {
        Map<String, Object> scenario = arenaScenarioStore.get(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("Arena scenario not found: " + scenarioId);
        }

        String runId = UUID.randomUUID().toString();
        Map<String, Object> run = new HashMap<>();
        run.put("runId", runId);
        run.put("scenarioId", scenarioId);
        run.put("status", "running");
        run.put("startedAt", LocalDateTime.now().toString());
        arenaRunStore.put(runId, run);

        log.info("[LabApplication] runArenaScenario scenarioId={} runId={}", scenarioId, runId);
        return run;
    }

    /**
     * 获取演练结果。
     *
     * @param runId 运行 ID
     * @return 运行结果
     */
    public Map<String, Object> getArenaRun(String runId) {
        return arenaRunStore.get(runId);
    }

    // ==================== 37. Skill Composer（技能组合器）====================

    /**
     * 获取技能图列表。
     *
     * @return 技能图列表
     */
    public List<Map<String, Object>> listSkillGraphs() {
        log.info("[LabApplication] listSkillGraphs");
        return new ArrayList<>(skillGraphStore.values());
    }

    /**
     * 保存技能图（创建或更新）。
     *
     * @param graphId   图 ID（为空时创建）
     * @param graphData 图数据
     * @return 保存后的图
     */
    public Map<String, Object> saveSkillGraph(String graphId, Map<String, Object> graphData) {
        String id = graphId != null && !graphId.isBlank() ? graphId : UUID.randomUUID().toString();
        Map<String, Object> graph = new HashMap<>(graphData);
        graph.put("id", id);
        graph.put("updatedAt", LocalDateTime.now().toString());
        if (!skillGraphStore.containsKey(id)) {
            graph.put("createdAt", LocalDateTime.now().toString());
        }
        skillGraphStore.put(id, graph);
        log.info("[LabApplication] saveSkillGraph id={}", id);
        return graph;
    }

    /**
     * 生成技能图执行计划（占位）。
     *
     * @param graphId 图 ID
     * @return 执行计划
     */
    public Map<String, Object> generateSkillGraphPlan(String graphId) {
        Map<String, Object> graph = skillGraphStore.get(graphId);
        if (graph == null) {
            throw new IllegalArgumentException("Skill graph not found: " + graphId);
        }

        log.info("[LabApplication] generateSkillGraphPlan graphId={}", graphId);
        Map<String, Object> plan = new HashMap<>();
        plan.put("planId", UUID.randomUUID().toString());
        plan.put("graphId", graphId);
        plan.put("status", "ready");
        plan.put("steps", List.of());
        plan.put("createdAt", LocalDateTime.now().toString());
        return plan;
    }

    /**
     * 执行技能图。
     *
     * @param graphId 图 ID
     * @return 运行状态
     */
    public Map<String, Object> executeSkillGraph(String graphId) {
        Map<String, Object> graph = skillGraphStore.get(graphId);
        if (graph == null) {
            throw new IllegalArgumentException("Skill graph not found: " + graphId);
        }

        String runId = UUID.randomUUID().toString();
        log.info("[LabApplication] executeSkillGraph graphId={} runId={}", graphId, runId);
        return Map.of("runId", runId, "status", "running");
    }

    // ==================== 38. Timeline（时间线回放）====================

    /**
     * 获取可回放会话列表（基于任务执行步骤）。
     *
     * @param targetType 对象类型筛选
     * @param targetId   对象 ID 筛选
     * @param status     状态筛选
     * @return 会话列表
     */
    public List<Map<String, Object>> listTimelineSessions(String targetType, String targetId, String status) {
        log.info("[LabApplication] listTimelineSessions targetType={} targetId={}", targetType, targetId);

        List<Task> tasks = getAllTasks();
        return tasks.stream()
                .filter(t -> {
                    if (targetId != null && !targetId.isBlank()) {
                        return targetId.equals(t.getId()) || targetId.equals(t.getWorkspaceId());
                    }
                    return true;
                })
                .filter(t -> {
                    if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                        return t.getStatus() != null && t.getStatus().name().equalsIgnoreCase(status);
                    }
                    return true;
                })
                .map(t -> {
                    Map<String, Object> session = new HashMap<>();
                    session.put("id", t.getId());
                    session.put("traceId", t.getId());
                    session.put("name", t.getName() != null ? t.getName() : "Task " + t.getId());
                    session.put("description", t.getDescription() != null ? t.getDescription() : "");
                    session.put("targetType", "task");
                    session.put("targetId", t.getId());
                    session.put("targetName", t.getName() != null ? t.getName() : "");
                    session.put("status", t.getStatus() != null ? t.getStatus().name().toLowerCase() : "pending");
                    session.put("startTime", t.getStartedAt() != null ? t.getStartedAt().toString() : "");
                    session.put("endTime", t.getCompletedAt() != null ? t.getCompletedAt().toString() : null);
                    int totalSteps = t.getExecutionSteps() != null ? t.getExecutionSteps().size() : 0;
                    session.put("totalSteps", totalSteps);
                    session.put("completedSteps", totalSteps);
                    session.put("currentStepIndex", totalSteps > 0 ? totalSteps - 1 : 0);
                    return session;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取会话详情（含所有步骤）。
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    public Map<String, Object> getTimelineSession(String sessionId) {
        log.info("[LabApplication] getTimelineSession sessionId={}", sessionId);

        Task task = getTaskStore().findById(sessionId).orElse(null);
        if (task == null) {
            return null;
        }

        Map<String, Object> session = new HashMap<>();
        session.put("id", task.getId());
        session.put("traceId", task.getId());
        session.put("name", task.getName() != null ? task.getName() : "Task " + task.getId());
        session.put("description", task.getDescription() != null ? task.getDescription() : "");
        session.put("targetType", "task");
        session.put("targetId", task.getId());
        session.put("targetName", task.getName() != null ? task.getName() : "");
        session.put("status", task.getStatus() != null ? task.getStatus().name().toLowerCase() : "pending");
        session.put("startTime", task.getStartedAt() != null ? task.getStartedAt().toString() : "");
        session.put("endTime", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);

        // 将执行步骤转换为 TimelineStep 格式
        List<Map<String, Object>> steps = new ArrayList<>();
        if (task.getExecutionSteps() != null) {
            for (int i = 0; i < task.getExecutionSteps().size(); i++) {
                Task.ExecutionStep step = task.getExecutionSteps().get(i);
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("id", task.getId() + "_step_" + i);
                stepMap.put("traceId", task.getId());
                stepMap.put("name", step.getStepType() != null ? step.getStepType() : "Step " + i);
                stepMap.put("targetType", "task");
                stepMap.put("targetId", task.getId());
                stepMap.put("targetName", task.getName() != null ? task.getName() : "");
                stepMap.put("eventType", step.getStepType() != null ? step.getStepType().toLowerCase() : "execute");
                stepMap.put("status", "completed");
                stepMap.put("startedAt", step.getTimestamp() != null ? step.getTimestamp().toString() : "");
                stepMap.put("completedAt", step.getTimestamp() != null ? step.getTimestamp().toString() : null);
                stepMap.put("duration", step.getDurationMs());
                stepMap.put("input", null);
                stepMap.put("output", step.getContent());
                stepMap.put("error", null);
                stepMap.put("parentId", null);
                stepMap.put("children", List.of());
                stepMap.put("message", step.getContent());
                stepMap.put("operator", task.getCharacterId());
                steps.add(stepMap);
            }
        }
        session.put("steps", steps);
        session.put("totalSteps", steps.size());
        session.put("completedSteps", steps.size());
        session.put("currentStepIndex", steps.isEmpty() ? 0 : steps.size() - 1);
        return session;
    }

    // ==================== 内部工具 ====================

    private List<Task> getAllTasks() {
        // 获取所有 Workspace 的任务
        List<Task> allTasks = new ArrayList<>();
        workspaceRegistry.listAll().forEach(ws -> {
            try {
                allTasks.addAll(getTaskStore().findByWorkspaceId(ws.getId()));
            } catch (Exception e) {
                log.warn("[LabApplication] Failed to load tasks for workspace {}: {}", ws.getId(), e.getMessage());
            }
        });
        return allTasks;
    }

    private Map<String, Object> toLabTaskMap(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", task.getId());
        map.put("workspaceId", task.getWorkspaceId());
        map.put("name", task.getName());
        map.put("description", task.getDescription());
        map.put("status", task.getStatus() != null ? task.getStatus().name().toLowerCase() : "pending");
        map.put("characterId", task.getCharacterId());
        map.put("createdAt", task.getCreatedAt() != null ? task.getCreatedAt().toString() : "");
        map.put("startTime", task.getStartedAt() != null ? task.getStartedAt().toString() : null);
        map.put("endTime", task.getCompletedAt() != null ? task.getCompletedAt().toString() : null);
        map.put("result", task.getResult());
        map.put("hasError", task.getErrorMessage() != null && !task.getErrorMessage().isBlank());

        // 添加任务验收相关字段（Lab 视图扩展）
        Map<String, Object> acceptance = new HashMap<>();
        acceptance.put("approved", task.getStatus() == TaskStatus.COMPLETED);
        acceptance.put("results", List.of());
        map.put("acceptance", acceptance);

        // schedule 信息（占位）
        map.put("schedule", Map.of("priority", "normal", "deadline", ""));
        // supervision 信息（占位）
        map.put("supervision", Map.of("mode", "auto"));
        return map;
    }
}

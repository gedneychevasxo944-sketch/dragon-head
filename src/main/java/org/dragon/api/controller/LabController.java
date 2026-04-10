package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.application.LabApplication;
import org.dragon.api.controller.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * LabController Lab 实验室模块 API
 *
 * <p>对应前端 /lab 页面，包含场景插件、任务网络、拓扑图、行为演练场、技能组合器、时间线回放等接口。
 * Base URL: /api/v1/lab
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Lab", description = "实验室模块：场景插件、任务网络、拓扑图、行为演练场、技能组合器、时间线回放")
@RestController
@RequestMapping("/api/v1/lab")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LabController {

    private final LabApplication labApplication;

    // ==================== 33. Scene Plugin（场景插件）====================

    /**
     * 33.1 获取场景列表
     * GET /api/v1/lab/scenes
     */
    @Operation(summary = "获取场景插件列表")
    @GetMapping("/scenes")
    public ApiResponse<List<Map<String, Object>>> listScenes() {
        List<Map<String, Object>> scenes = labApplication.listScenes();
        return ApiResponse.success(scenes);
    }

    /**
     * 33.2 创建场景
     * POST /api/v1/lab/scenes
     */
    @Operation(summary = "创建场景")
    @PostMapping("/scenes")
    public ApiResponse<Map<String, Object>> createScene(@RequestBody Map<String, Object> sceneData) {
        Map<String, Object> scene = labApplication.createScene(sceneData);
        return ApiResponse.success(scene);
    }

    /**
     * 33.2 更新场景
     * PUT /api/v1/lab/scenes/:id
     */
    @Operation(summary = "更新场景")
    @PutMapping("/scenes/{id}")
    public ApiResponse<Map<String, Object>> updateScene(
            @PathVariable String id,
            @RequestBody Map<String, Object> sceneData) {
        try {
            Map<String, Object> scene = labApplication.updateScene(id, sceneData);
            return ApiResponse.success(scene);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    /**
     * 33.3 获取场景实例列表
     * GET /api/v1/lab/scenes/:sceneId/instances
     */
    @Operation(summary = "获取场景实例列表")
    @GetMapping("/scenes/{sceneId}/instances")
    public ApiResponse<List<Map<String, Object>>> listSceneInstances(@PathVariable String sceneId) {
        List<Map<String, Object>> instances = labApplication.listSceneInstances(sceneId);
        return ApiResponse.success(instances);
    }

    // ==================== 34. Task Network（任务网络）====================

    /**
     * 34.1 获取任务列表（全局任务网络视图）
     * GET /api/v1/lab/tasks
     */
    @Operation(summary = "获取任务网络列表")
    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> listLabTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        List<Map<String, Object>> tasks = labApplication.listLabTasks(status, priority);
        return ApiResponse.success(tasks);
    }

    /**
     * 34.2 获取任务详情
     * GET /api/v1/lab/tasks/:id
     */
    @Operation(summary = "获取任务详情")
    @GetMapping("/tasks/{id}")
    public ApiResponse<Map<String, Object>> getLabTask(@PathVariable String id) {
        Map<String, Object> task = labApplication.getLabTask(id);
        if (task == null) {
            return ApiResponse.error(404, "Task not found: " + id);
        }
        return ApiResponse.success(task);
    }

    /**
     * 34.3 更新任务状态
     * PUT /api/v1/lab/tasks/:id/status
     */
    @Operation(summary = "更新任务状态")
    @PutMapping("/tasks/{id}/status")
    public ApiResponse<Map<String, Object>> updateLabTaskStatus(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            String newStatus = (String) body.get("status");
            if (newStatus == null || newStatus.isBlank()) {
                return ApiResponse.error(400, "status is required");
            }
            Map<String, Object> task = labApplication.updateLabTaskStatus(id, newStatus);
            return ApiResponse.success(task);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    /**
     * 34.4 提交验收结果
     * POST /api/v1/lab/tasks/:id/acceptance
     */
    @Operation(summary = "提交任务验收结果")
    @PostMapping("/tasks/{id}/acceptance")
    public ApiResponse<Map<String, Object>> submitTaskAcceptance(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> result = labApplication.submitTaskAcceptance(id, body);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    // ==================== 35. Topology（拓扑图）====================

    /**
     * 35.1 获取拓扑图数据
     * GET /api/v1/lab/topology
     */
    @Operation(summary = "获取系统拓扑图数据")
    @GetMapping("/topology")
    public ApiResponse<Map<String, Object>> getTopology(
            @RequestParam(required = false) String nodeTypes,
            @RequestParam(required = false) String edgeTypes,
            @RequestParam(required = false) String workspaceId) {
        Map<String, Object> topology = labApplication.getTopology(nodeTypes, edgeTypes, workspaceId);
        return ApiResponse.success(topology);
    }

    // ==================== 36. Behavior Arena（行为演练场）====================

    /**
     * 36.1 获取演练场景列表
     * GET /api/v1/lab/arena/scenarios
     */
    @Operation(summary = "获取行为演练场景列表")
    @GetMapping("/arena/scenarios")
    public ApiResponse<List<Map<String, Object>>> listArenaScenarios() {
        List<Map<String, Object>> scenarios = labApplication.listArenaScenarios();
        return ApiResponse.success(scenarios);
    }

    /**
     * 36.2 创建演练场景
     * POST /api/v1/lab/arena/scenarios
     */
    @Operation(summary = "创建行为演练场景")
    @PostMapping("/arena/scenarios")
    public ApiResponse<Map<String, Object>> createArenaScenario(@RequestBody Map<String, Object> scenarioData) {
        Map<String, Object> scenario = labApplication.createArenaScenario(scenarioData);
        return ApiResponse.success(scenario);
    }

    /**
     * 36.3 运行演练
     * POST /api/v1/lab/arena/scenarios/:scenarioId/run
     */
    @Operation(summary = "运行行为演练场景")
    @PostMapping("/arena/scenarios/{scenarioId}/run")
    public ApiResponse<Map<String, Object>> runArenaScenario(@PathVariable String scenarioId) {
        try {
            Map<String, Object> run = labApplication.runArenaScenario(scenarioId);
            return ApiResponse.success(run);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    /**
     * 36.4 获取演练结果
     * GET /api/v1/lab/arena/runs/:runId
     */
    @Operation(summary = "获取演练运行结果")
    @GetMapping("/arena/runs/{runId}")
    public ApiResponse<Map<String, Object>> getArenaRun(@PathVariable String runId) {
        Map<String, Object> run = labApplication.getArenaRun(runId);
        if (run == null) {
            return ApiResponse.error(404, "Arena run not found: " + runId);
        }
        return ApiResponse.success(run);
    }

    // ==================== 37. Skill Composer（技能组合器）====================

    /**
     * 37.1 获取技能图列表
     * GET /api/v1/lab/skill-graphs
     */
    @Operation(summary = "获取技能图列表")
    @GetMapping("/skill-graphs")
    public ApiResponse<List<Map<String, Object>>> listSkillGraphs() {
        List<Map<String, Object>> graphs = labApplication.listSkillGraphs();
        return ApiResponse.success(graphs);
    }

    /**
     * 37.2 创建技能图
     * POST /api/v1/lab/skill-graphs
     */
    @Operation(summary = "创建技能图")
    @PostMapping("/skill-graphs")
    public ApiResponse<Map<String, Object>> createSkillGraph(@RequestBody Map<String, Object> graphData) {
        Map<String, Object> graph = labApplication.saveSkillGraph(null, graphData);
        return ApiResponse.success(graph);
    }

    /**
     * 37.2 更新技能图
     * PUT /api/v1/lab/skill-graphs/:id
     */
    @Operation(summary = "更新技能图")
    @PutMapping("/skill-graphs/{id}")
    public ApiResponse<Map<String, Object>> updateSkillGraph(
            @PathVariable String id,
            @RequestBody Map<String, Object> graphData) {
        Map<String, Object> graph = labApplication.saveSkillGraph(id, graphData);
        return ApiResponse.success(graph);
    }

    /**
     * 37.3 生成执行计划
     * POST /api/v1/lab/skill-graphs/:id/plan
     */
    @Operation(summary = "生成技能图执行计划")
    @PostMapping("/skill-graphs/{id}/plan")
    public ApiResponse<Map<String, Object>> generateSkillGraphPlan(@PathVariable String id) {
        try {
            Map<String, Object> plan = labApplication.generateSkillGraphPlan(id);
            return ApiResponse.success(plan);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    /**
     * 37.4 执行技能图
     * POST /api/v1/lab/skill-graphs/:id/execute
     */
    @Operation(summary = "执行技能图")
    @PostMapping("/skill-graphs/{id}/execute")
    public ApiResponse<Map<String, Object>> executeSkillGraph(@PathVariable String id) {
        try {
            Map<String, Object> result = labApplication.executeSkillGraph(id);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    // ==================== 38. Timeline（时间线回放）====================

    /**
     * 38.1 获取可回放会话列表
     * GET /api/v1/lab/timeline/sessions
     */
    @Operation(summary = "获取时间线回放会话列表")
    @GetMapping("/timeline/sessions")
    public ApiResponse<List<Map<String, Object>>> listTimelineSessions(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> sessions = labApplication.listTimelineSessions(targetType, targetId, status);
        return ApiResponse.success(sessions);
    }

    /**
     * 38.2 获取会话详情（含所有步骤）
     * GET /api/v1/lab/timeline/sessions/:sessionId
     */
    @Operation(summary = "获取时间线会话详情（含 TimelineStep 树）")
    @GetMapping("/timeline/sessions/{sessionId}")
    public ApiResponse<Map<String, Object>> getTimelineSession(@PathVariable String sessionId) {
        Map<String, Object> session = labApplication.getTimelineSession(sessionId);
        if (session == null) {
            return ApiResponse.error(404, "Timeline session not found: " + sessionId);
        }
        return ApiResponse.success(session);
    }
}

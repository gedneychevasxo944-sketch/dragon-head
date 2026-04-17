package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.permission.checker.PermissionChecker;
import org.dragon.task.Task;
import org.dragon.task.TaskExecutionService;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.task.WorkspaceTaskService;
import org.dragon.workspace.task.dto.TaskCreationCommand;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * TaskController - 任务统一管理入口
 *
 * <p>提供任务的 CRUD 和执行接口：
 * <ul>
 *   <li>任务查询（列表、详情）</li>
 *   <li>任务状态管理（取消、暂停、恢复）</li>
 *   <li>任务执行（提交新任务、恢复任务）</li>
 * </ul>
 *
 * @author yijunw
 */
@Tag(name = "Task", description = "任务管理：CRUD、执行、状态管理")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/tasks")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TaskController {

    private final WorkspaceTaskService workspaceTaskService;
    private final TaskExecutionService taskExecutionService;
    private final PermissionChecker permissionChecker;

    // ==================== 任务查询 ====================

    /**
     * 获取任务列表
     * GET /api/v1/workspaces/{workspaceId}/tasks
     */
    @Operation(summary = "获取任务列表")
    @GetMapping
    public ApiResponse<PageResponse<Task>> listTasks(
            @PathVariable String workspaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        List<Task> all;
        if (status != null && !status.isBlank()) {
            try {
                TaskStatus ts = TaskStatus.valueOf(status.toUpperCase());
                all = workspaceTaskService.listTasksByStatus(workspaceId, ts);
            } catch (Exception e) {
                all = workspaceTaskService.listTasks(workspaceId);
            }
        } else {
            all = workspaceTaskService.listTasks(workspaceId);
        }

        long total = all.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, all.size());
        List<Task> pageData = fromIndex >= all.size() ? List.of() : all.subList(fromIndex, toIndex);
        return ApiResponse.success(PageResponse.of(pageData, total, page, pageSize));
    }

    /**
     * 获取任务详情
     * GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}
     */
    @Operation(summary = "获取任务详情")
    @GetMapping("/{taskId}")
    public ApiResponse<Task> getTask(
            @PathVariable String workspaceId,
            @PathVariable String taskId) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        return workspaceTaskService.getTask(workspaceId, taskId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Task not found"));
    }

    /**
     * 获取任务结果
     * GET /api/v1/workspaces/{workspaceId}/tasks/{taskId}/result
     */
    @Operation(summary = "获取任务结果")
    @GetMapping("/{taskId}/result")
    public ApiResponse<String> getTaskResult(
            @PathVariable String workspaceId,
            @PathVariable String taskId) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        try {
            String result = workspaceTaskService.getTaskResult(workspaceId, taskId);
            return ApiResponse.success(result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 任务状态管理 ====================

    /**
     * 取消任务
     * POST /api/v1/workspaces/{workspaceId}/tasks/{taskId}/cancel
     */
    @Operation(summary = "取消任务")
    @PostMapping("/{taskId}/cancel")
    public ApiResponse<Task> cancelTask(
            @PathVariable String workspaceId,
            @PathVariable String taskId) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        try {
            Task task = workspaceTaskService.cancelTask(workspaceId, taskId);
            return ApiResponse.success(task);
        } catch (IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 暂停任务
     * POST /api/v1/workspaces/{workspaceId}/tasks/{taskId}/suspend
     */
    @Operation(summary = "暂停任务")
    @PostMapping("/{taskId}/suspend")
    public ApiResponse<Task> suspendTask(
            @PathVariable String workspaceId,
            @PathVariable String taskId,
            @RequestBody SuspendTaskRequest request) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        Task task = workspaceTaskService.suspendTask(workspaceId, taskId, request.getReason());
        return ApiResponse.success(task);
    }

    /**
     * 恢复任务
     * POST /api/v1/workspaces/{workspaceId}/tasks/{taskId}/resume
     */
    @Operation(summary = "恢复任务")
    @PostMapping("/{taskId}/resume")
    public ApiResponse<Task> resumeTask(
            @PathVariable String workspaceId,
            @PathVariable String taskId,
            @RequestBody(required = false) ResumeTaskRequest request) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        Object newInput = request != null ? request.getNewInput() : null;
        Task task = workspaceTaskService.resumeTask(workspaceId, taskId, newInput);

        // 执行任务
        taskExecutionService.resumeAndExecute(workspaceId, task,
                request != null && request.getMessage() != null ? request.getMessage() : null);

        return ApiResponse.success(task);
    }

    // ==================== 任务执行 ====================

    /**
     * 提交新任务
     * POST /api/v1/workspaces/{workspaceId}/tasks/submit
     */
    @Operation(summary = "提交新任务")
    @PostMapping("/submit")
    public ApiResponse<Task> submitTask(
            @PathVariable String workspaceId,
            @RequestBody SubmitTaskRequest request) {
        permissionChecker.checkUse("WORKSPACE", workspaceId);

        Task task = taskExecutionService.submitAndExecute(workspaceId, request.toCommand());
        return ApiResponse.success(task);
    }

    // ==================== Request DTOs ====================

    @Data
    public static class SubmitTaskRequest {
        private String taskName;
        private String taskDescription;
        private String input;
        private String creatorId;
        private String sourceChannel;
        private String sourceChatId;
        private String sourceMessageId;
        private Map<String, Object> metadata;

        public TaskCreationCommand toCommand() {
            return TaskCreationCommand.builder()
                    .taskName(taskName)
                    .taskDescription(taskDescription)
                    .input(input)
                    .creatorId(creatorId)
                    .sourceChannel(sourceChannel)
                    .sourceChatId(sourceChatId)
                    .sourceMessageId(sourceMessageId)
                    .metadata(metadata)
                    .build();
        }
    }

    @Data
    public static class SuspendTaskRequest {
        private String reason;
    }

    @Data
    public static class ResumeTaskRequest {
        private Object newInput;
        private NormalizedMessage message;
    }
}
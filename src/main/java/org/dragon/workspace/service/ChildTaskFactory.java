package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.workspace.service.dto.ChildTaskPlan;
import org.dragon.workspace.service.dto.TaskDecompositionResult;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 子任务工厂
 * 负责将分解结果转换为真实 Task 实体，并处理依赖映射
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ChildTaskFactory {

    /**
     * 从分解结果创建子任务列表
     * 三阶段：
     * 1. 为每个 ChildTaskPlan 创建真实 Task
     * 2. 建立 planTaskId -> taskId 映射
     * 3. 把 dependencyPlanTaskIds 转成真实 dependencyTaskIds
     */
    public List<Task> createChildTasks(TaskDecompositionResult result, Task parentTask) {
        if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
            log.warn("[ChildTaskFactory] No child tasks in decomposition result");
            return List.of();
        }

        List<ChildTaskPlan> plans = result.getChildTasks();
        List<Task> tasks = new ArrayList<>();

        // 阶段1：创建所有 Task（此时 dependencyTaskIds 还是 LLM 输出的 planTaskId）
        for (ChildTaskPlan plan : plans) {
            Task task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .parentTaskId(parentTask.getId())
                    .workspaceId(parentTask.getWorkspaceId())
                    .name(plan.getName())
                    .description(plan.getDescription())
                    .characterId(plan.getCharacterId())
                    .input(parentTask.getInput())
                    .status(TaskStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            tasks.add(task);
        }

        // 阶段2：建立 planTaskId -> taskId 映射
        Map<String, String> planIdMapping = buildPlanIdMapping(plans, tasks);

        // 阶段3：解析依赖 planTaskIds 为真实 taskIds
        resolveDependencyPlanTaskIds(plans, tasks, planIdMapping);

        return tasks;
    }

    /**
     * 建立 planTaskId -> taskId 映射
     */
    private Map<String, String> buildPlanIdMapping(List<ChildTaskPlan> plans, List<Task> tasks) {
        Map<String, String> mapping = new java.util.HashMap<>();
        for (int i = 0; i < plans.size(); i++) {
            String planId = plans.get(i).getPlanTaskId();
            if (planId != null && !planId.isEmpty()) {
                mapping.put(planId, tasks.get(i).getId());
            }
        }
        return mapping;
    }

    /**
     * 将 dependencyPlanTaskIds 解析为真实的 dependencyTaskIds
     */
    private void resolveDependencyPlanTaskIds(List<ChildTaskPlan> plans, List<Task> tasks, Map<String, String> planIdMapping) {
        for (int i = 0; i < plans.size(); i++) {
            List<String> planDeps = plans.get(i).getDependencyPlanTaskIds();
            if (planDeps == null || planDeps.isEmpty()) {
                continue;
            }
            List<String> resolvedDeps = new ArrayList<>();
            for (String planDepId : planDeps) {
                String resolvedId = planIdMapping.get(planDepId);
                if (resolvedId != null) {
                    resolvedDeps.add(resolvedId);
                }
            }
            tasks.get(i).setDependencyTaskIds(resolvedDeps);
        }
    }
}

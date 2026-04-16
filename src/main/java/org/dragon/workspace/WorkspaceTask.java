package org.dragon.workspace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.plugin.WorkspacePlugin;
import org.dragon.workspace.plugin.WorkspacePluginRegistry;
import org.dragon.workspace.step.Step;
import org.dragon.workspace.step.StepRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WorkspaceTask - Workspace 下的任务执行实例
 *
 * <p>持有 Step 有向图，通过拓扑排序执行，支持并行和循环。
 *
 * @author yijunw
 */
@Slf4j
@RequiredArgsConstructor
public class WorkspaceTask {

    private final String id;
    private final String workspaceId;
    private final StepRegistry stepRegistry;
    private final WorkspacePluginRegistry pluginRegistry;

    /**
     * Step 名称 -> Step 实例
     */
    private final Map<String, Step> steps = new HashMap<>();

    /**
     * stepName -> 依赖的 stepNames
     */
    private final Map<String, Set<String>> dependencies = new HashMap<>();

    /**
     * stepName -> 依赖它的 stepNames（反向索引，用于循环依赖）
     */
    private final Map<String, Set<String>> dependents = new HashMap<>();

    /**
     * 循环中止条件
     */
    private TerminationCondition terminationCondition;

    /**
     * 添加 Step 及其依赖关系
     */
    public void addStep(Step step, Set<String> dependsOn) {
        steps.put(step.getName(), step);
        Set<String> deps = dependsOn != null ? new HashSet<>(dependsOn) : new HashSet<>();
        dependencies.put(step.getName(), deps);
        // 更新 dependents（反向索引）
        for (String dep : deps) {
            dependents.computeIfAbsent(dep, k -> new HashSet<>()).add(step.getName());
        }
    }

    /**
     * 设置循环中止条件
     */
    public void setTerminationCondition(TerminationCondition condition) {
        this.terminationCondition = condition;
    }

    /**
     * 执行 Step 图（拓扑排序，并行可并行的 Step）
     */
    public TaskContext execute(TaskContext ctx) {
        log.info("[WorkspaceTask] Starting execution for task {} with {} steps", id, steps.size());
        long startTime = System.currentTimeMillis();

        Set<String> completed = new HashSet<>();
        Map<String, StepResult> results = new HashMap<>();
        ctx.setStepResults(new HashMap<>());

        int iteration = 0;
        while (!shouldTerminate(completed, results, ctx)) {
            iteration++;
            if (iteration > 1000) {
                log.warn("[WorkspaceTask] Exceeded max iterations, terminating");
                break;
            }

            // 找所有依赖已满足且未执行的 Step
            Set<String> runnable = findRunnableSteps(completed);
            if (runnable.isEmpty()) {
                log.info("[WorkspaceTask] No runnable steps, terminating");
                break;
            }

            log.info("[WorkspaceTask] Iteration {}, running steps: {}", iteration, runnable);
            // 并行执行可并行的 Steps
            List<StepResult> batchResults = runnable.stream()
                    .map(name -> executeStep(steps.get(name), ctx))
                    .collect(Collectors.toList());

            // 收集结果，更新完成状态
            for (StepResult r : batchResults) {
                results.put(r.getStepName(), r);
                completed.add(r.getStepName());
                ctx.recordStepResult(r.getStepName(), r);
            }

            // 触发依赖此 Step 的 Step 重新执行（循环支持）
            triggerDependents(completed, r -> {
                // 循环：将完成的 Step 从 completed 中移除，使其可重新执行
                completed.remove(r.getStepName());
                log.info("[WorkspaceTask] Loop: re-enabling step {} for re-execution", r.getStepName());
            }, results);
        }

        log.info("[WorkspaceTask] Execution completed in {}ms, {} iterations", System.currentTimeMillis() - startTime, iteration);
        return ctx;
    }

    /**
     * 执行单个 Step（含 Plugin 拦截）
     */
    private StepResult executeStep(Step step, TaskContext ctx) {
        final String stepName = step.getName();
        final Step currentStep = step;
        long startTime = System.currentTimeMillis();

        // 调用 before 拦截
        getPluginsForStep(stepName).forEach(p -> p.beforeStep(ctx, currentStep));

        // 调用 in 拦截
        getPluginsForStep(stepName).forEach(p -> p.inStep(ctx, currentStep));

        // 调用 step.execute()
        StepResult result = doExecuteStep(currentStep, ctx, stepName, startTime);

        // 调用 after 拦截（无论成功失败都调用）
        getPluginsForStep(stepName).forEach(p -> p.afterStep(ctx, currentStep, result));

        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    private StepResult doExecuteStep(Step step, TaskContext ctx, String stepName, long startTime) {
        try {
            if (!step.isEnabled(ctx)) {
                log.info("[WorkspaceTask] Step {} is disabled, skipping", stepName);
                return StepResult.builder()
                        .stepName(stepName)
                        .success(true)
                        .output("skipped")
                        .durationMs(0)
                        .build();
            }
            return step.execute(ctx);
        } catch (Exception e) {
            log.error("[WorkspaceTask] Step {} execution failed: {}", stepName, e.getMessage(), e);
            return StepResult.builder()
                    .stepName(stepName)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 找所有依赖已满足且未执行的 Step
     */
    private Set<String> findRunnableSteps(Set<String> completed) {
        return steps.keySet().stream()
                .filter(name -> !completed.contains(name))
                .filter(name -> dependencies.get(name).isEmpty() || completed.containsAll(dependencies.get(name)))
                .collect(Collectors.toSet());
    }

    /**
     * 判断是否应该终止
     */
    private boolean shouldTerminate(Set<String> completed, Map<String, StepResult> results, TaskContext ctx) {
        if (terminationCondition != null && terminationCondition.isMet(completed, results, ctx)) {
            log.info("[WorkspaceTask] Termination condition met");
            return true;
        }
        // 所有 Step 完成
        return completed.containsAll(steps.keySet());
    }

    /**
     * 触发依赖某个 Step 的所有 Step 重新执行
     */
    private void triggerDependents(Set<String> completed, java.util.function.Consumer<StepResult> onReTrigger, Map<String, StepResult> results) {
        Set<String> justCompleted = new HashSet<>(completed);
        for (String stepName : justCompleted) {
            Set<String> dependentSet = dependents.get(stepName);
            if (dependentSet != null) {
                for (String dependent : dependentSet) {
                    StepResult result = results.get(stepName);
                    if (result != null && result.isSuccess() && needsReTrigger(result)) {
                        onReTrigger.accept(result);
                    }
                }
            }
        }
    }

    /**
     * 判断某个 Step 的结果是否需要触发依赖方重新执行
     */
    private boolean needsReTrigger(StepResult result) {
        // 当 Step 返回 WAITING_DEPENDENCY 等状态时，触发依赖检查
        if (result.getOutput() != null && result.getOutput().toString().contains("WAITING")) {
            return true;
        }
        return false;
    }

    /**
     * 获取某个 Step 关联的所有 Plugin
     */
    private List<WorkspacePlugin> getPluginsForStep(String stepName) {
        if (pluginRegistry == null) {
            return List.of();
        }
        return pluginRegistry.getPlugins(stepName);
    }

    /**
     * 构建执行上下文
     */
    public TaskContext buildContext() {
        return TaskContext.builder()
                .workspaceId(workspaceId)
                .build();
    }

    /**
     * 循环中止条件接口
     */
    @FunctionalInterface
    public interface TerminationCondition {
        boolean isMet(Set<String> completed, Map<String, StepResult> results, TaskContext ctx);
    }

    /**
     * 创建一个新的 WorkspaceTask 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String workspaceId;
        private StepRegistry stepRegistry;
        private WorkspacePluginRegistry pluginRegistry;

        public Builder id(String id) { this.id = id; return this; }
        public Builder workspaceId(String workspaceId) { this.workspaceId = workspaceId; return this; }
        public Builder stepRegistry(StepRegistry stepRegistry) { this.stepRegistry = stepRegistry; return this; }
        public Builder pluginRegistry(WorkspacePluginRegistry pluginRegistry) { this.pluginRegistry = pluginRegistry; return this; }

        public WorkspaceTask build() {
            return new WorkspaceTask(id, workspaceId, stepRegistry, pluginRegistry);
        }
    }
}

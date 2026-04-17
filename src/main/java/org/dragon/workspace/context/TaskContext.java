package org.dragon.workspace.context;

import java.util.List;
import java.util.Map;

import org.dragon.step.ExecutionContext;
import org.dragon.step.StepResult;
import org.dragon.task.Task;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.member.WorkspaceMember;

import lombok.Builder;
import lombok.Data;

/**
 * TaskContext - 贯穿整个执行链路
 *
 * <p>执行时存于内存，不入库。需要持久化的内容（Task/StepResult）已有对应存储。
 *
 * @author yijunw
 */
@Data
@Builder
public class TaskContext implements ExecutionContext {

    /**
     * 执行 ID
     */
    private String executionId;

    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * 当前任务（信息容器）
     */
    private Task task;

    /**
     * 当前Task下所有任务
     */
    private Map<String, Task> taskMap;

    /**
     * taskId -> characterId 映射
     */
    private Map<String, String> taskExecutorMap;

    /**
     * step 执行结果: <taskId, <stepName, result>>
     */
    private Map<String, Map<String, StepResult>> stepResults;

    /**
     * 协作信息容器
     */
    private ChatRoom chatRoom;

    /**
     * Workspace 实体
     */
    private Workspace workspace;

    /**
     * Workspace 成员列表
     */
    private List<WorkspaceMember> members;

    /**
     * 执行配置
     */
    private Map<String, Object> config;

    /**
     * 父任务 ID（可选）
     */
    private String parentTaskId;

    /**
     * 物料上下文
     */
    private String materialContext;

    /**
     * 是否完成
     */
    @Builder.Default
    private boolean complete = false;

    /**
     * 最终响应
     */
    private String finalResponse;

    // ==================== ExecutionContext 实现 ====================

    @Override
    public String getCharacterId() {
        return task != null ? task.getCharacterId() : null;
    }

    @Override
    public int getCurrentIteration() {
        Object iteration = config != null ? config.get("currentIteration") : null;
        return iteration != null ? (Integer) iteration : 0;
    }

    @Override
    public void setCurrentIteration(int iteration) {
        setConfigValue("currentIteration", iteration);
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void complete(String response) {
        this.complete = true;
        this.finalResponse = response;
    }

    /**
     * 设置配置值（用于 DecisionStep 动态设置后续 Step）
     */
    public void setConfigValue(String key, Object value) {
        if (this.config == null) {
            this.config = new java.util.HashMap<>();
        }
        this.config.put(key, value);
    }

    /**
     * 获取配置值
     */
    public Object getConfigValue(String key) {
        return this.config != null ? this.config.get(key) : null;
    }

    /**
     * 记录某个 Step 的执行结果
     */
    public void recordStepResult(String stepName, StepResult result) {
        if (this.stepResults == null) {
            this.stepResults = new java.util.HashMap<>();
        }
        String taskId = this.task != null ? this.task.getId() : "root";
        this.stepResults.computeIfAbsent(taskId, k -> new java.util.HashMap<>())
                .put(stepName, result);
    }

    /**
     * 获取当前任务的 Step 结果
     */
    public Map<String, StepResult> getStepResultsForCurrentTask() {
        if (this.stepResults == null || this.task == null) {
            return Map.of();
        }
        return stepResults.getOrDefault(this.task.getId(), Map.of());
    }
}

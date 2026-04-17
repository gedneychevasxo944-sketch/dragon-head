package org.dragon.step;

import java.util.Map;

import org.dragon.task.Task;

/**
 * Step 执行上下文接口
 *
 * <p>Workspace 和 Character 共用的执行上下文，屏蔽具体实现差异。
 *
 * @author yijunw
 */
public interface ExecutionContext {

    /**
     * 获取执行 ID
     */
    String getExecutionId();

    /**
     * 获取 Character ID
     */
    String getCharacterId();

    /**
     * 获取 Workspace ID
     */
    String getWorkspaceId();

    /**
     * 获取当前迭代次数
     */
    int getCurrentIteration();

    /**
     * 设置当前迭代次数
     */
    void setCurrentIteration(int iteration);

    /**
     * 是否完成
     */
    boolean isComplete();

    /**
     * 标记完成
     */
    void complete(String response);

    /**
     * 获取配置值
     */
    Object getConfigValue(String key);

    /**
     * 设置配置值
     */
    void setConfigValue(String key, Object value);

    /**
     * 记录 Step 执行结果
     */
    void recordStepResult(String stepName, StepResult result);

    /**
     * 获取当前任务的 Step 结果
     */
    Map<String, StepResult> getStepResultsForCurrentTask();

    // ==================== Workspace-specific methods with defaults ====================
    // These have defaults so ReActContext doesn't need to implement them

    /**
     * 获取当前任务（Workspace 场景使用，ReAct 场景返回 null）
     */
    default Task getTask() {
        return null;
    }

    /**
     * 获取原始配置对象（Workspace 场景使用，ReAct 场景返回 null）
     */
    default Map<String, Object> getConfig() {
        return null;
    }

    /**
     * 获取 ChatRoom（Workspace 场景使用，ReAct 场景返回 null）
     */
    default Object getChatRoom() {
        return null;
    }

    /**
     * 获取成员列表（Workspace 场景使用，ReAct 场景返回 null）
     */
    default Object getMembers() {
        return null;
    }
}
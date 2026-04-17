package org.dragon.workspace.context;

import java.util.List;
import java.util.Map;

import org.dragon.step.ExecutionContext;

/**
 * TaskContext 配置 Key 定义
 *
 * <p>统一管理 TaskContext.config 的所有配置项，避免字符串 key 散落各处。
 * 类似 {@link org.dragon.config.store.ConfigKey} 的模式。
 *
 * @author yijunw
 */
public final class TaskContextConfig {

    private TaskContextConfig() {
        // 工具类禁止实例化
    }

    // ==================== ResumeStep ====================

    /**
     * 续跑决策结果
     * @see org.dragon.workspace.step.ResumeStep
     */
    public static final String CONTINUATION_DECISION = "continuationDecision";

    /**
     * 续跑目标任务 ID
     */
    public static final String TARGET_TASK_ID = "targetTaskId";

    /**
     * 续跑目标任务类型：CHILD / PARENT
     */
    public static final String TARGET_TASK_TYPE = "targetTaskType";

    // ==================== DecomposeStep ====================

    /**
     * 分解后的子任务列表
     * @see org.dragon.workspace.step.DecomposeStep
     */
    public static final String CHILD_TASKS = "childTasks";

    // ==================== DecisionStep ====================

    /**
     * 后续需要执行的 Step 名称列表
     */
    public static final String NEXT_STEPS = "nextSteps";

    /**
     * 循环配置
     */
    public static final String LOOP = "loop";

    /**
     * 是否终止执行
     */
    public static final String TERMINATE = "terminate";

    // ==================== WorkspaceTaskExecutor ====================

    /**
     * 是否为恢复执行（续跑已有任务）
     */
    public static final String RESUME = "resume";

    // ==================== 便捷方法 ====================

    /**
     * 设置续跑决策相关配置
     */
    public static void setContinuationConfig(ExecutionContext ctx, String decision, String targetTaskId, String targetTaskType) {
        ctx.setConfigValue(CONTINUATION_DECISION, decision);
        ctx.setConfigValue(TARGET_TASK_ID, targetTaskId);
        ctx.setConfigValue(TARGET_TASK_TYPE, targetTaskType);
    }

    /**
     * 设置分解后的子任务
     */
    public static void setChildTasks(ExecutionContext ctx, List<?> childTasks) {
        ctx.setConfigValue(CHILD_TASKS, childTasks);
    }

    /**
     * 设置决策结果
     */
    public static void setDecisionConfig(ExecutionContext ctx, List<String> nextSteps, Map<String, Object> loop, Boolean terminate) {
        if (nextSteps != null) {
            ctx.setConfigValue(NEXT_STEPS, nextSteps);
        }
        if (loop != null) {
            ctx.setConfigValue(LOOP, loop);
        }
        if (terminate != null) {
            ctx.setConfigValue(TERMINATE, terminate);
        }
    }
}
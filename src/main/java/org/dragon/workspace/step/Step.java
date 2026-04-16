package org.dragon.workspace.step;

import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;

/**
 * Step 接口 - Workspace 执行链路的基本单元
 *
 * <p>每个 Step 代表执行链路中的一个步骤，可独立执行、可被 Plugin 拦截。
 *
 * @author yijunw
 */
public interface Step {

    /**
     * 获取 Step 名称
     */
    String getName();

    /**
     * 执行 Step
     */
    StepResult execute(TaskContext ctx);

    /**
     * 执行前拦截（入口拦截）
     * <p>在 execute() 调用之前执行，可用于参数校验、日志记录等。
     */
    default void beforeExecute(TaskContext ctx) {
    }

    /**
     * 执行中拦截（可修改输入/输出）
     * <p>在 execute() 调用过程中执行，可用于修改输入参数或处理中间状态。
     */
    default void inExecute(TaskContext ctx) {
    }

    /**
     * 执行后拦截（结果拦截）
     * <p>在 execute() 调用之后执行，可用于结果记录、后置处理等。
     */
    default void afterExecute(TaskContext ctx, StepResult result) {
    }

    /**
     * 判断 Step 是否启用
     * <p>默认启用，返回 false 时跳过此 Step。
     */
    default boolean isEnabled(TaskContext ctx) {
        return true;
    }
}

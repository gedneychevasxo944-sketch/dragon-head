package org.dragon.step;

import java.util.Map;

/**
 * Step 接口 - 执行链路的基本单元
 *
 * <p>每个 Step 代表执行链路中的一个步骤，可独立执行、可组合成 DAG。
 * Workspace 和 Character 共用此接口。
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
    StepResult execute(ExecutionContext ctx);

    /**
     * 执行前拦截（入口拦截）
     */
    default void beforeExecute(ExecutionContext ctx) {
    }

    /**
     * 执行中拦截（可修改输入/输出）
     */
    default void inExecute(ExecutionContext ctx) {
    }

    /**
     * 执行后拦截（结果拦截）
     */
    default void afterExecute(ExecutionContext ctx, StepResult result) {
    }

    /**
     * 判断 Step 是否启用
     */
    default boolean isEnabled(ExecutionContext ctx) {
        return true;
    }
}
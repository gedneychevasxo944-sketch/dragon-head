package org.dragon.workspace.plugin;

import java.util.List;

import org.dragon.step.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.step.Step;

/**
 * WorkspacePlugin 接口
 *
 * <p>通过 before/in/after 拦截 WorkspaceTask 中的 Step 执行。
 *
 * @author yijunw
 */
public interface WorkspacePlugin {

    /**
     * 获取插件名称
     */
    String getName();

    /**
     * 获取插件作用的 Step 名称列表
     */
    List<String> getTargetSteps();

    /**
     * 执行前拦截
     */
    default void beforeStep(TaskContext ctx, Step step) {
    }

    /**
     * 执行中拦截（可修改输入/输出）
     */
    default void inStep(TaskContext ctx, Step step) {
    }

    /**
     * 执行后拦截
     */
    default void afterStep(TaskContext ctx, Step step, StepResult result) {
    }

    /**
     * 判断插件是否启用
     */
    default boolean isEnabled() {
        return true;
    }
}

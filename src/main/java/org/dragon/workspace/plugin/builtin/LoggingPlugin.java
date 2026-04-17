package org.dragon.workspace.plugin.builtin;

import java.util.List;

import org.dragon.step.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.plugin.WorkspacePlugin;
import org.dragon.step.Step;

import lombok.extern.slf4j.Slf4j;

/**
 * 内置日志记录插件
 *
 * <p>在每个 Step 执行前/后记录日志。
 *
 * @author yijunw
 */
@Slf4j
public class LoggingPlugin implements WorkspacePlugin {

    @Override
    public String getName() {
        return "logging";
    }

    @Override
    public List<String> getTargetSteps() {
        // 作用于所有 Step
        return List.of();
    }

    @Override
    public void beforeStep(TaskContext ctx, Step step) {
        log.info("[LoggingPlugin] Before step: {} in task {}",
                step.getName(), ctx.getTask() != null ? ctx.getTask().getId() : "unknown");
    }

    @Override
    public void afterStep(TaskContext ctx, Step step, StepResult result) {
        log.info("[LoggingPlugin] After step: {}, success={}, duration={}ms",
                step.getName(), result.isSuccess(), result.getDurationMs());
    }
}

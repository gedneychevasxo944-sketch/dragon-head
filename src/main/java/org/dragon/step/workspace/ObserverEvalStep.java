package org.dragon.step.workspace;

import java.util.List;

import org.dragon.application.ObserverApplication;
import org.dragon.observer.Observer;
import org.dragon.observer.store.ObserverStore;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.step.StepResult;
import org.dragon.step.Step;
import org.dragon.step.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ObserverEvalStep - 触发 Observer 评价
 *
 * <p>在任务执行后触发 Observer 对该任务执行情况进行评价。
 * Observer 会根据配置评估 Character 的表现，并可能生成改进建议。
 *
 * <p>只有当 Workspace 关联了 Observer 时才会触发评价。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObserverEvalStep implements Step {

    private final StoreFactory storeFactory;
    private final ObserverApplication observerApplication;

    @Override
    public String getName() {
        return "observerEval";
    }

    @Override
    public StepResult execute(ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();
        Task task = ctx.getTask();
        String workspaceId = ctx.getWorkspaceId();

        if (task == null || workspaceId == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(true)
                    .output("skipped")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            // 获取 Workspace 关联的 Observers
            ObserverStore observerStore = storeFactory.get(ObserverStore.class);
            List<Observer> observers = observerStore.findByWorkspaceId(workspaceId);

            if (observers.isEmpty()) {
                log.debug("[ObserveStep] No observers in workspace {}", workspaceId);
                return StepResult.builder()
                        .stepName(getName())
                        .input(task.getId())
                        .output("no observers")
                        .success(true)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // 触发每个 Observer 评价
            for (Observer observer : observers) {
                try {
                    observerApplication.triggerEvaluation(observer.getId());
                } catch (Exception e) {
                    log.warn("[ObserveStep] Failed to trigger observer {}: {}", observer.getId(), e.getMessage());
                }
            }

            return StepResult.builder()
                    .stepName(getName())
                    .input(task.getId())
                    .output("triggered " + observers.size() + " observers")
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[ObserveStep] Observe failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }
}

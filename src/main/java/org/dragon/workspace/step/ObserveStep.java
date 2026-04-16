package org.dragon.workspace.step;

import java.util.List;

import org.dragon.application.ObserverApplication;
import org.dragon.observer.Observer;
import org.dragon.observer.store.ObserverStore;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ObserveStep - 触发 Observer 评价
 *
 * <p>新增加的 Step，在任务执行后触发 Observer 评价。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObserveStep implements Step {

    private final StoreFactory storeFactory;
    private final ObserverApplication observerApplication;

    @Override
    public String getName() {
        return "observe";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
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

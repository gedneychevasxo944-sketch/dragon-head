package org.dragon.step.workspace;

import java.util.List;

import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.step.StepResult;
import org.dragon.step.Step;
import org.dragon.step.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ClaimStep - 认领自己发布的未处理需求
 *
 * <p>在 ExecuteStep 之后执行。当 Character 执行完任务后，检查自己之前发布的需求
 * 是否有人认领。如果没人认领，就自己认领回来继续处理。
 *
 * <p>典型场景：Character A 发布了一个需求给其他人，但没人接单，
 * 那么 A 在完成当前任务后，应该认领回这个需求。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimStep implements Step {

    private final StoreFactory storeFactory;

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public StepResult execute(ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();
        String characterId = ctx.getTask() != null ? ctx.getTask().getCharacterId() : null;

        if (characterId == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // 查找当前 Character 发布的所有需求中未被认领的
        List<Task> myPublishedDemands = findMyPublishedDemands(characterId, ctx.getWorkspaceId());

        for (Task demand : myPublishedDemands) {
            // 检查是否已有认领者
            if (demand.getClaimerIds() != null && !demand.getClaimerIds().isEmpty()) {
                continue;
            }

            // 无人认领 → 认领
            demand.addClaimerId(characterId);
            storeFactory.get(TaskStore.class).update(demand);

            // 发布 CLAIMED 响应
            ((ChatRoom) ctx.getChatRoom()).publishClaimed(
                    ctx.getWorkspaceId(),
                    ctx.getTask().getId(),  // 当前任务的 taskId
                    characterId,
                    demand.getId()  // inResponseTo = demand 的 taskId
            );

            log.info("[ClaimStep] Character {} claimed demand {} published by {}",
                    characterId, demand.getId(), demand.getOriginalCharacterId());
        }

        return StepResult.builder()
                .stepName(getName())
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 查找当前 Character 发布的需求中未被认领的
     */
    private List<Task> findMyPublishedDemands(String characterId, String workspaceId) {
        return storeFactory.get(TaskStore.class).findByOriginalCharacterId(characterId, workspaceId);
    }
}
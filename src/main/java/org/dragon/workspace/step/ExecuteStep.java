package org.dragon.workspace.step;

import java.time.LocalDateTime;
import java.util.Map;

import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.store.StoreFactory;
import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import org.dragon.task.TaskStore;
import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;
import org.dragon.workspace.step.DependencyStep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ExecuteStep - 调用 Character.run() 执行任务
 *
 * <p>从 DefaultTaskBridge 迁移而来。
 * Step 只调用 Character.run()，ReActExecutor 完全在 Character 内部执行。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteStep implements Step {

    private final CharacterRegistry characterRegistry;
    private final CharacterRuntimeBinder characterRuntimeBinder;
    private final StoreFactory storeFactory;
    private final ChatRoom chatRoom;
    private final DependencyStep dependencyStep;

    @Override
    public String getName() {
        return "execute";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
        long startTime = System.currentTimeMillis();
        Task task = ctx.getTask();

        if (task == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage("No task in context")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        String characterId = task.getCharacterId();
        if (characterId == null || characterId.isEmpty()) {
            return failResult(task, "No character assigned", startTime);
        }

        // 获取 Character
        Character character = characterRegistry.get(characterId)
                .orElseThrow(() -> new IllegalStateException("Character not found: " + characterId));

        // 绑定运行时依赖
        characterRuntimeBinder.bind(character, ctx.getWorkspaceId());

        // 更新任务状态为 RUNNING
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        storeFactory.get(TaskStore.class).update(task);

        try {
            // 将任务输入转为字符串
            String userInput = getUserInput(task);

            // 调用 Character.run() 执行（简单调用，不暴露 ReAct 细节）
            String output = character.run(userInput);

            // 解析输出类型：result 或 demand/request
            OutputType outputType = parseOutputType(output);
            task.setResult(output);

            if (outputType == OutputType.DEMAND) {
                // 是需求/请求：设置为等待状态，并发布请求到 ChatRoom
                task.setStatus(TaskStatus.WAITING_DEPENDENCY);
                task.setWaitingReason("waiting_for_response");
                task.setUpdatedAt(LocalDateTime.now());
                storeFactory.get(TaskStore.class).update(task);

                // 发布需求到 ChatRoom
                chatRoom.publishRequest(ctx.getWorkspaceId(), task.getId(), characterId, output);

                log.info("[ExecuteStep] Task {} published demand, waiting for response", task.getId());
                return StepResult.builder()
                        .stepName(getName())
                        .input(task.getId())
                        .output(output)
                        .success(true)
                        .waitingForResponse(true)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            } else {
                // 是结果：设置为完成状态
                task.setStatus(TaskStatus.COMPLETED);
                task.setCompletedAt(LocalDateTime.now());
                storeFactory.get(TaskStore.class).update(task);

                // 发布结果到 ChatRoom（甩出去不管）
                chatRoom.publishResult(ctx.getWorkspaceId(), task.getId(), characterId, output);

                // 通知依赖解决，唤醒等待此结果的任务
                dependencyStep.notifyDependencyResolved(task.getId());

                log.info("[ExecuteStep] Task {} executed successfully on character {}", task.getId(), characterId);
                return StepResult.builder()
                        .stepName(getName())
                        .input(task.getId())
                        .output(output)
                        .success(true)
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

        } catch (Exception e) {
            log.error("[ExecuteStep] Task {} execution failed: {}", task.getId(), e.getMessage(), e);
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            storeFactory.get(TaskStore.class).update(task);
            return StepResult.builder()
                    .stepName(getName())
                    .input(task.getId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private String getUserInput(Task task) {
        Object input = task.getInput();
        if (input instanceof NormalizedMessage) {
            return ((NormalizedMessage) input).getTextContent();
        }
        return input != null ? input.toString() : "";
    }

    private StepResult failResult(Task task, String error, long startTime) {
        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(error);
        storeFactory.get(TaskStore.class).update(task);
        return StepResult.builder()
                .stepName(getName())
                .input(task.getId())
                .success(false)
                .errorMessage(error)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private Map<String, Object> buildOutput(String status, String reason) {
        return Map.of("status", status, "reason", reason);
    }

    /**
     * 输出类型：结果或需求/请求
     */
    private enum OutputType {
        RESULT,   // 正常结果
        DEMAND    // 需求/请求（需要等待响应）
    }

    /**
     * 解析输出类型
     * <p>简单判断：如果输出包含 "[REQUEST]" 或 "[DEMAND]" 前缀，则认为是需求
     */
    private OutputType parseOutputType(String output) {
        if (output == null || output.isBlank()) {
            return OutputType.RESULT;
        }
        String trimmed = output.trim();
        if (trimmed.startsWith("[REQUEST]") || trimmed.startsWith("[DEMAND]")) {
            return OutputType.DEMAND;
        }
        return OutputType.RESULT;
    }
}

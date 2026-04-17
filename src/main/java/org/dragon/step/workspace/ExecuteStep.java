package org.dragon.step.workspace;

import java.time.LocalDateTime;
import java.util.HashSet;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.channel.entity.NormalizedMessage;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.runtime.CharacterRuntime;
import org.dragon.character.mind.TraitResolutionService;
import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.runtime.SkillRegistry;
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
 * ExecuteStep - 调用 Character.run() 执行任务
 *
 * <p>Step 只调用 Character.run()，ReAct 循环完全在 Character 内部执行。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteStep implements Step {

    private final CharacterRegistry characterRegistry;
    private final ConfigApplication configApplication;
    private final ModelRegistry modelRegistry;
    private final SkillRegistry skillRegistry;
    private final TraitResolutionService traitResolutionService;
    private final AssetAssociationService assetAssociationService;
    private final StoreFactory storeFactory;

    @Override
    public String getName() {
        return "execute";
    }

    @Override
    public StepResult execute(ExecutionContext ctx) {
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
        Long workspaceId = ctx.getWorkspaceId() != null ? Long.parseLong(ctx.getWorkspaceId()) : null;
        bindRuntime(character, workspaceId);

        // 更新任务状态为 RUNNING
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        storeFactory.get(TaskStore.class).update(task);

        try {
            String userInput = getUserInput(task);
            String output = character.run(userInput);

            OutputType outputType = parseOutputType(output);
            task.setResult(output);

            if (outputType == OutputType.DEMAND) {
                task.setStatus(TaskStatus.WAITING_DEPENDENCY);
                task.setWaitingReason("waiting_for_response");
                task.setOriginalCharacterId(characterId);
                task.setUpdatedAt(LocalDateTime.now());
                storeFactory.get(TaskStore.class).update(task);

                ((ChatRoom) ctx.getChatRoom()).publishDemand(ctx.getWorkspaceId(), task.getId(), characterId,
                        task.getId(), task.getDescription(), null);

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
                task.setStatus(TaskStatus.COMPLETED);
                task.setCompletedAt(LocalDateTime.now());
                storeFactory.get(TaskStore.class).update(task);

                ((ChatRoom) ctx.getChatRoom()).publishCompleted(ctx.getWorkspaceId(), task.getId(), characterId,
                        task.getParentTaskId(), output);

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

    private void bindRuntime(Character character, Long workspaceId) {
        CharacterRuntime runtime = CharacterRuntime.builder()
                .configApplication(configApplication)
                .modelRegistry(modelRegistry)
                .mind(null)
                .workspaceId(workspaceId)
                .skillRegistry(skillRegistry)
                .traitResolutionService(traitResolutionService)
                .assetAssociationService(assetAssociationService)
                .build();

        character.setRuntime(runtime);

        if (character.getAllowedTools() == null) {
            character.setAllowedTools(new HashSet<>());
        }

        log.info("[ExecuteStep] Bound runtime for character {} in workspace {}",
                character.getId(), workspaceId);
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

    private enum OutputType {
        RESULT,
        DEMAND
    }

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
package org.dragon.step.workspace;

import org.dragon.workspace.cooperation.chat.ChatRoom;
import org.dragon.step.StepResult;
import org.dragon.step.Step;
import org.dragon.step.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ResultStep - 发布执行结果到 ChatRoom
 *
 * <p>将任务执行结果发布到 ChatRoom，分两种类型：
 * <ul>
 *   <li>DEMAND - 输出是需求/请求，发布到聊天室等待其他 Character 认领</li>
 *   <li>RESULT - 输出是正常响应，发布到聊天室通知相关方</li>
 * </ul>
 *
 * <p>通过输出是否包含 [DEMAND] 或 [REQUEST] 前缀来区分类型。
 *
 * @author yijunw
 */
@Slf4j
@Service
public class ResultStep implements Step {

    @Override
    public String getName() {
        return "result";
    }

    @Override
    public StepResult execute(ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();

        // 从 ExecuteStep 获取输出
        StepResult executeResult = ctx.getStepResultsForCurrentTask().get("execute");
        if (executeResult == null) {
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage("No execute result found")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        String output = executeResult.getOutput() != null ? executeResult.getOutput().toString() : "";

        // 解析输出类型
        OutputType outputType = parseOutputType(output);

        if (outputType == OutputType.DEMAND) {
            // 发布需求
            return publishDemand(ctx, output, startTime);
        } else if (outputType == OutputType.RESULT) {
            // 发布结果（响应某个需求）
            return publishResult(ctx, output, startTime);
        } else {
            // 普通结果，不需要发布
            return StepResult.builder()
                    .stepName(getName())
                    .output(output)
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private StepResult publishDemand(ExecutionContext ctx, String output, long startTime) {
        String taskId = ctx.getTask().getId();
        String characterId = ctx.getTask().getCharacterId();
        String workspaceId = ctx.getWorkspaceId();

        // 解析指定执行者（如果有）
        String targetCharacterId = parseAssignedCharacter(output);

        // 发布需求
        ((ChatRoom) ctx.getChatRoom()).publishDemand(
                workspaceId,
                taskId,
                characterId,
                taskId,
                ctx.getTask().getDescription(),
                targetCharacterId
        );

        log.info("[ResultStep] Published demand from task {} by {}", taskId, characterId);

        return StepResult.builder()
                .stepName(getName())
                .output(output)
                .success(true)
                .waitingForResponse(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private StepResult publishResult(ExecutionContext ctx, String output, long startTime) {
        String taskId = ctx.getTask().getId();
        String characterId = ctx.getTask().getCharacterId();
        String workspaceId = ctx.getWorkspaceId();
        String parentTaskId = ctx.getTask().getParentTaskId();

        // 发布结果
        ((ChatRoom) ctx.getChatRoom()).publishCompleted(
                workspaceId,
                taskId,
                characterId,
                parentTaskId,
                output
        );

        log.info("[ResultStep] Published result from task {} to parent {}", taskId, parentTaskId);

        return StepResult.builder()
                .stepName(getName())
                .output(output)
                .success(true)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 输出类型
     */
    private enum OutputType {
        RESULT,   // 正常结果
        DEMAND    // 需求/请求
    }

    /**
     * 解析输出类型
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

    /**
     * 从输出中解析指定执行者
     * <p>简单判断：如果包含 "@character:xxx" 格式，则认为是指定执行者
     */
    private String parseAssignedCharacter(String output) {
        if (output == null) {
            return null;
        }
        // 简单实现，实际可能需要更复杂的解析
        if (output.contains("@")) {
            // TODO: 实现更复杂的解析逻辑
        }
        return null;
    }
}
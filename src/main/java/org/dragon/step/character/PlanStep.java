package org.dragon.step.character;

import java.util.List;
import java.util.Map;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.config.service.ConfigApplication;
import org.dragon.step.Step;
import org.dragon.step.StepResult;
import org.dragon.step.ExecutionContext;
import org.dragon.workspace.context.TaskContextConfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PlanStep - 任务执行计划生成
 *
 * <p>在 Character 被分配任务后、进入 ReAct 循环前执行。
 * 调用 prompt_writer Character 生成任务执行计划（action points），
 * 将复杂任务拆解为可执行的具体步骤。
 *
 * <p>生成的 action points 会存入上下文，供后续步骤使用。
 *
 * @author yijunw
 */
@Slf4j
@RequiredArgsConstructor
public class PlanStep implements Step {

    private static final String DEFAULT_PLAN_CHARACTER = "prompt_writer";
    private static final String DEFAULT_PROMPT = "请为以下任务制定执行计划，输出 JSON 格式：{\"actionPoints\": [\"步骤1\", \"步骤2\", ...]}";

    private final CharacterCaller characterCaller;
    private final CharacterRegistry characterRegistry;
    private final ConfigApplication configApplication;

    @Override
    public String getName() {
        return "plan";
    }

    @Override
    public StepResult execute(ExecutionContext ctx) {
        long startTime = System.currentTimeMillis();

        String taskDescription = ctx.getTask() != null ? ctx.getTask().getDescription() : null;
        if (taskDescription == null || taskDescription.isBlank()) {
            return StepResult.builder()
                    .stepName(getName())
                    .output("no task description")
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            // 调用 LLM 生成计划
            String planPrompt = DEFAULT_PROMPT + "\n\n任务：" + taskDescription;
            Character planChar = characterRegistry.get(DEFAULT_PLAN_CHARACTER).orElse(null);
            if (planChar == null) {
                return StepResult.builder()
                        .stepName(getName())
                        .success(false)
                        .errorMessage("Plan character not found")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            String planResult = characterCaller.call(planChar, planPrompt);

            // 将计划设置到上下文，供后续 ReAct 使用
            List<String> actionPoints = parseActionPoints(planResult);
            TaskContextConfig.setDecisionConfig(ctx, actionPoints, null, false);

            log.info("[PlanStep] Generated {} action points for task", actionPoints.size());

            return StepResult.builder()
                    .stepName(getName())
                    .input(taskDescription)
                    .output(Map.of("actionPoints", actionPoints, "rawResult", planResult))
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[PlanStep] Planning failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 解析 LLM 返回的计划，提取 action points
     */
    @SuppressWarnings("unchecked")
    private List<String> parseActionPoints(String planResult) {
        try {
            JsonObject json = new Gson().fromJson(planResult, JsonObject.class);
            if (json != null && json.has("actionPoints")) {
                return new Gson().fromJson(json.get("actionPoints"), List.class);
            }
        } catch (Exception e) {
            log.warn("[PlanStep] Failed to parse action points: {}", e.getMessage());
        }
        return List.of();
    }
}
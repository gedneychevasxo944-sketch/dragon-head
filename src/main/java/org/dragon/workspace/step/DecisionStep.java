package org.dragon.workspace.step;

import java.util.List;
import java.util.Map;

import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.workspace.context.StepResult;
import org.dragon.workspace.context.TaskContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DecisionStep - 根据 LLM 输出动态决定后续 Step
 *
 * <p>调用 LLM（通过 CharacterCaller），根据 OutputSchema 约束的输出格式，
 * 动态决定下一步要执行的 Step。
 *
 * @author yijunw
 */
@Slf4j
@RequiredArgsConstructor
public class DecisionStep implements Step {

    private static final String DEFAULT_DECISION_CHARACTER = "prompt_writer";

    private final CharacterCaller characterCaller;
    private final CharacterRegistry characterRegistry;
    private final OutputSchema schema;
    private final String prompt;
    private final String decisionCharacterId;

    public DecisionStep(CharacterCaller characterCaller, CharacterRegistry characterRegistry,
                       OutputSchema schema, String prompt) {
        this(characterCaller, characterRegistry, schema, prompt, DEFAULT_DECISION_CHARACTER);
    }

    @Override
    public String getName() {
        return "decision";
    }

    @Override
    public StepResult execute(TaskContext ctx) {
        try {
            // 1. 获取决策用 Character
            Character decisionChar = getDecisionCharacter();

            // 2. 调用 LLM，传入 schema 约束输出格式
            String llmResponse = characterCaller.call(decisionChar, prompt);

            // 3. 解析结构化输出
            StepPlan plan = parseStepPlan(llmResponse);

            // 4. 设置后续 Step 到上下文
            if (plan.getNextSteps() != null && !plan.getNextSteps().isEmpty()) {
                ctx.setConfigValue("nextSteps", plan.getNextSteps());
            }
            if (plan.getLoop() != null) {
                ctx.setConfigValue("loop", plan.getLoop());
            }
            if (plan.getTerminate() != null) {
                ctx.setConfigValue("terminate", plan.getTerminate());
            }

            log.info("[DecisionStep] Decision made: nextSteps={}, loop={}, terminate={}",
                    plan.getNextSteps(), plan.getLoop(), plan.getTerminate());

            return StepResult.builder()
                    .stepName(getName())
                    .input(prompt)
                    .output(plan)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("[DecisionStep] Decision failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .input(prompt)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Character getDecisionCharacter() {
        if (decisionCharacterId != null) {
            return characterRegistry.get(decisionCharacterId)
                    .orElseThrow(() -> new IllegalStateException("Decision character not found: " + decisionCharacterId));
        }
        return characterRegistry.get(DEFAULT_DECISION_CHARACTER)
                .orElseThrow(() -> new IllegalStateException("Default decision character not found: " + DEFAULT_DECISION_CHARACTER));
    }

    @SuppressWarnings("unchecked")
    private StepPlan parseStepPlan(String llmResponse) {
        StepPlan plan = new StepPlan();
        try {
            JsonObject json = new Gson().fromJson(llmResponse, JsonObject.class);
            if (json == null) {
                return plan;
            }

            if (json.has("nextSteps")) {
                plan.setNextSteps(new Gson().fromJson(json.get("nextSteps"), List.class));
            }
            if (json.has("parameters")) {
                plan.setParameters(new Gson().fromJson(json.get("parameters"), Map.class));
            }
            if (json.has("loop")) {
                plan.setLoop(new Gson().fromJson(json.get("loop"), Map.class));
            }
            if (json.has("terminate")) {
                plan.setTerminate(json.get("terminate").getAsBoolean());
            }
        } catch (Exception e) {
            log.warn("[DecisionStep] Failed to parse LLM response as JSON: {}", e.getMessage());
        }
        return plan;
    }

    /**
     * Step 计划，包含下一步 Step 名称、参数、循环条件
     */
    @lombok.Data
    public static class StepPlan {
        private List<String> nextSteps;
        private Map<String, Object> parameters;
        private Map<String, Object> loop;
        private Boolean terminate;
    }
}

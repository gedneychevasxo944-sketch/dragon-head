package org.dragon.step.character;

import java.util.List;
import java.util.Map;

import org.dragon.agent.react.ReActContext;
import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.step.StepResult;
import org.dragon.step.ExecutionContext;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ClarifyStep - 推测用户可能的追问
 *
 * <p>在 ReAct 循环之前执行。根据用户当前的输入，通过 LLM 分析并推测
 * 用户可能想要追问的后续问题，生成候选问题列表供用户选择。
 *
 * <p>这是一个可选功能，通过配置控制：
 * <ul>
 *   <li>`character.clarify.enabled` - 是否启用（默认关闭）</li>
 * </ul>
 *
 * <p>典型场景：用户问了一个模糊的问题，ClarifyStep 可以帮助
 * 推测用户真正想问的是什么，提供几个可能的追问选项。
 *
 * @author yijunw
 */
@Slf4j
public class ClarifyStep extends CharacterStep {

    private static final String DEFAULT_CLARIFY_CHARACTER = "prompt_writer";
    private static final String DEFAULT_PROMPT = "根据用户的问题，推测用户可能想要追问的后续问题。\n" +
            "输出 JSON 格式：{\"clarifications\": [\"问题1\", \"问题2\", \"问题3\"]}\n" +
            "请只输出 JSON，不要有其他内容。";

    private final CharacterCaller characterCaller;
    private final CharacterRegistry characterRegistry;

    public ClarifyStep(CharacterCaller characterCaller, CharacterRegistry characterRegistry) {
        super("clarify");
        this.characterCaller = characterCaller;
        this.characterRegistry = characterRegistry;
    }

    public ClarifyStep() {
        super("clarify");
        this.characterCaller = null;
        this.characterRegistry = null;
    }

    @Override
    public boolean isEnabled(ExecutionContext ctx) {
        if (!(ctx instanceof ReActContext)) {
            return false;
        }
        ReActContext reactCtx = (ReActContext) ctx;
        // 从上下文的配置中读取开关
        Object enabled = reactCtx.getConfigValue("character.clarify.enabled");
        if (enabled != null) {
            if (enabled instanceof Boolean) {
                return (Boolean) enabled;
            }
            if (enabled instanceof String) {
                return Boolean.parseBoolean((String) enabled);
            }
        }
        // 默认不启用
        return false;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        long startTime = System.currentTimeMillis();

        String userInput = ctx.getUserInput();
        if (userInput == null || userInput.isBlank()) {
            return StepResult.builder()
                    .stepName(getName())
                    .output("no user input")
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        try {
            // 调用 LLM 生成可能的追问
            String prompt = DEFAULT_PROMPT + "\n\n用户问题：" + userInput;
            Character clarifyChar = characterRegistry.get(DEFAULT_CLARIFY_CHARACTER).orElse(null);
            if (clarifyChar == null) {
                return StepResult.builder()
                        .stepName(getName())
                        .success(false)
                        .errorMessage("Clarify character not found")
                        .durationMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            String result = characterCaller.call(clarifyChar, prompt);

            // 解析结果
            List<String> clarifications = parseClarifications(result);

            // 存储到上下文，供后续使用
            ctx.setConfigValue("clarifications", clarifications);

            log.info("[ClarifyStep] Generated {} clarifications for user input", clarifications.size());

            return StepResult.builder()
                    .stepName(getName())
                    .input(userInput)
                    .output(Map.of("clarifications", clarifications, "rawResult", result))
                    .success(true)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            log.error("[ClarifyStep] Clarify failed: {}", e.getMessage(), e);
            return StepResult.builder()
                    .stepName(getName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 解析 LLM 返回的结果，提取 clarifications
     */
    @SuppressWarnings("unchecked")
    private List<String> parseClarifications(String result) {
        try {
            JsonObject json = new Gson().fromJson(result, JsonObject.class);
            if (json != null && json.has("clarifications")) {
                return new Gson().fromJson(json.get("clarifications"), List.class);
            }
        } catch (Exception e) {
            log.warn("[ClarifyStep] Failed to parse clarifications: {}", e.getMessage());
        }
        return List.of();
    }
}
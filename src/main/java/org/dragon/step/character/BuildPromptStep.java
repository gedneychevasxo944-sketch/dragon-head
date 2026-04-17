package org.dragon.step.character;

import java.util.HashMap;
import java.util.Map;

import org.dragon.agent.react.ReActContext;
import org.dragon.agent.react.ThoughtPromptAssembler;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.dragon.step.StepResult;
import org.dragon.workspace.task.dto.PromptWriterInput;
import org.dragon.agent.llm.util.CharacterCaller;
import org.springframework.beans.factory.ObjectProvider;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * BuildPromptStep - 思考 Prompt 组装
 *
 * <p>在 ThinkStep 之前执行，构建完整的 LLM 输入 Prompt。
 *
 * <p>组装过程：
 * <ol>
 *   <li>优先调用 prompt_writer Character 动态装配（通过 PromptWriterInput）</li>
 *   <li>回退到 ThoughtPromptAssembler 手动拼装</li>
 * </ol>
 *
 * <p>组装的 Prompt 包含：用户输入、任务描述、历史记忆、可用工具等信息。
 * 组装结果通过 context.setConfigValue() 传递给 ThinkStep。
 *
 * @author yijunw
 */
@Slf4j
public class BuildPromptStep extends CharacterStep {

    private static final String BUILT_PROMPT_KEY = "builtPrompt";

    private final ConfigApplication configApplication;
    private final ObjectProvider<CharacterRegistry> characterRegistryProvider;
    private final ObjectProvider<CharacterCaller> characterCallerProvider;
    private final ThoughtPromptAssembler thoughtPromptAssembler;
    private final Gson gson;

    public BuildPromptStep(ConfigApplication configApplication,
                          ObjectProvider<CharacterRegistry> characterRegistryProvider,
                          ObjectProvider<CharacterCaller> characterCallerProvider,
                          ThoughtPromptAssembler thoughtPromptAssembler) {
        super("buildPrompt");
        this.configApplication = configApplication;
        this.characterRegistryProvider = characterRegistryProvider;
        this.characterCallerProvider = characterCallerProvider;
        this.thoughtPromptAssembler = thoughtPromptAssembler;
        this.gson = new Gson();
    }

    public BuildPromptStep() {
        super("buildPrompt");
        this.configApplication = null;
        this.characterRegistryProvider = null;
        this.characterCallerProvider = null;
        this.thoughtPromptAssembler = null;
        this.gson = null;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        String prompt = buildPrompt(ctx);

        // 存储到 context，供后续 ThinkStep 使用
        ctx.setConfigValue(BUILT_PROMPT_KEY, prompt);

        return StepResult.success(getName(), prompt);
    }

    private String buildPrompt(ReActContext ctx) {
        // 优先尝试动态装配
        String dynamicPrompt = tryBuildDynamicThoughtPrompt(ctx);
        if (dynamicPrompt != null) {
            return dynamicPrompt;
        }
        // 回退到 ThoughtPromptAssembler 拼装
        if (thoughtPromptAssembler != null) {
            return thoughtPromptAssembler.assemble(ctx);
        }
        // 最终回退到 userInput
        return ctx.getUserInput();
    }

    private String tryBuildDynamicThoughtPrompt(ReActContext ctx) {
        if (configApplication == null) {
            return null;
        }

        CharacterRegistry characterRegistry = characterRegistryProvider != null
                ? characterRegistryProvider.getIfAvailable() : null;
        CharacterCaller characterCaller = characterCallerProvider != null
                ? characterCallerProvider.getIfAvailable() : null;
        if (characterRegistry == null || characterCaller == null) {
            return null;
        }

        try {
            String workspaceId = resolveWorkspaceId(ctx);
            if (workspaceId == null) {
                return null;
            }

            // 获取 PromptWriter Character
            Character promptWriterChar = characterRegistry.get("prompt_writer").orElse(null);
            if (promptWriterChar == null) {
                return null;
            }

            // 获取模板
            String template = configApplication.getPrompt(workspaceId, ctx.getCharacterId(), PromptKeys.REACT_EXECUTE);
            if (template == null || template.isEmpty()) {
                return null;
            }

            // 组装 PromptWriter 输入
            PromptWriterInput input = buildReActPromptWriterInput(ctx, template);
            String inputJson = gson.toJson(input);

            // 调用 PromptWriter Character 生成最终 Prompt
            return characterCaller.call(promptWriterChar, inputJson);

        } catch (Exception e) {
            log.warn("[BuildPromptStep] PromptWriter dynamic prompt failed, falling back to assembler: {}",
                    e.getMessage());
            return null;
        }
    }

    private String resolveWorkspaceId(ReActContext ctx) {
        if (ctx.getWorkspaceId() != null) {
            return ctx.getWorkspaceId();
        }
        var task = ctx.getTask();
        if (task != null && task.getWorkspaceId() != null) {
            return task.getWorkspaceId();
        }
        return null;
    }

    private PromptWriterInput buildReActPromptWriterInput(ReActContext ctx, String template) {
        var memberInfos = java.util.Collections.<PromptWriterInput.MemberInfo>emptyList();

        Map<String, Object> contextHints = new HashMap<>();
        contextHints.put("timestamp", java.time.LocalDateTime.now().toString());
        contextHints.put("allowFollowUp", false);
        contextHints.put("executionId", ctx.getExecutionId());

        PromptWriterInput.TaskInfo taskInfo = PromptWriterInput.TaskInfo.builder()
                .id(ctx.getTask() != null ? ctx.getTask().getId() : null)
                .name(null)
                .description(null)
                .input(ctx.getUserInput())
                .parentTaskId(null)
                .build();

        return PromptWriterInput.builder()
                .workspaceId(resolveWorkspaceId(ctx))
                .promptType("react_execute")
                .promptTemplate(template)
                .task(taskInfo)
                .members(memberInfos)
                .contextHints(contextHints)
                .build();
    }

    /**
     * 获取 BuildPromptStep 构建的 Prompt
     */
    public static String getBuiltPrompt(ReActContext ctx) {
        Object prompt = ctx.getConfigValue(BUILT_PROMPT_KEY);
        return prompt != null ? prompt.toString() : null;
    }
}
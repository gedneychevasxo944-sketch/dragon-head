package org.dragon.tool.runtime.tools;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 向用户提问工具实现。
 *
 * <p>对应 TypeScript 版本的 {@code src/tools/AskUserQuestionTool/AskUserQuestionTool.tsx}。
 * Agent 可以向用户提出结构化的多选题，等待用户回答后继续执行。
 *
 * <h3>功能：</h3>
 * <ul>
 *   <li>支持 1-4 个问题，每个问题 2-4 个选项</li>
 *   <li>支持多选（multiSelect）</li>
 *   <li>接收用户回答后写入 Output</li>
 * </ul>
 *
 * <h3>注意：</h3>
 * <p>在云端 API 场景下，实际的用户交互通过 SSE/WebSocket 回调实现。
 * 本实现中 call() 方法接收预填的 answers，由调用方在权限检查阶段填入用户回答。
 *
 * <h3>输入 Schema：</h3>
 * <pre>
 * {
 *   "questions": [
 *     {
 *       "question": "Which approach do you prefer?",
 *       "header": "Approach",
 *       "options": [
 *         { "label": "Option A", "description": "Description A" },
 *         { "label": "Option B", "description": "Description B" }
 *       ],
 *       "multiSelect": false
 *     }
 *   ],
 *   "answers": { "Which approach do you prefer?": "Option A" }
 * }
 * </pre>
 */
@Slf4j
@Component
public class AskUserQuestionTool extends AbstractTool<AskUserQuestionTool.Input, AskUserQuestionTool.Output> {

    private static final long MAX_RESULT_SIZE = 100_000;

    public AskUserQuestionTool() {
        super("AskUserQuestion",
                "Ask the user a question with multiple-choice options and wait for their response.",
                Input.class);
    }

    // ── 核心方法 ─────────────────────────────────────────────────────────

    @Override
    protected CompletableFuture<ToolResult<Output>> doCall(Input input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            List<Question> questions = input.getQuestions();
            // answers 由权限检查/用户交互阶段填入
            Map<String, String> answers = input.getAnswers() != null
                    ? input.getAnswers()
                    : new LinkedHashMap<>();

            log.info("[AskUserQuestionTool] 提问: questions={}, answersProvided={}",
                    questions != null ? questions.size() : 0, answers.size());

            return ToolResult.ok(Output.builder()
                    .questions(questions)
                    .answers(answers)
                    .build());
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(Output output, String toolUseId) {
        Map<String, String> answers = output.getAnswers() != null
                ? output.getAnswers()
                : Map.of();

        if (answers.isEmpty()) {
            return ToolResultBlockParam.ofText(toolUseId,
                    "User has not answered the questions yet.");
        }

        StringJoiner sj = new StringJoiner(", ");
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            sj.add("\"" + entry.getKey() + "\"=\"" + entry.getValue() + "\"");
        }

        return ToolResultBlockParam.ofText(toolUseId,
                "User has answered your questions: " + sj +
                ". You can now continue with the user's answers in mind.");
    }

    // ── 元信息 ───────────────────────────────────────────────────────────

    @Override
    public boolean isReadOnly(Input input) {
        return true;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;
    }

    @Override
    public boolean requiresUserInteraction() {
        return true;
    }

    @Override
    public long getMaxResultSizeChars() {
        return MAX_RESULT_SIZE;
    }

    @Override
    public String getSearchHint() {
        return "prompt the user with a multiple-choice question";
    }

    @Override
    public String getUserFacingName(Input input) {
        return "";
    }

    // ── 输入输出类型 ─────────────────────────────────────────────────────

    @Data
    @Builder
    public static class QuestionOption {
        private String label;
        private String description;
    }

    @Data
    @Builder
    public static class Question {
        private String question;
        private String header;
        private List<QuestionOption> options;
        private Boolean multiSelect;
    }

    @Data
    @Builder
    public static class Input {
        private List<Question> questions;
        /** 由调用方（权限层/用户交互层）填入的答案 */
        private Map<String, String> answers;
    }

    @Data
    @Builder
    public static class Output {
        private List<Question> questions;
        private Map<String, String> answers;
    }
}

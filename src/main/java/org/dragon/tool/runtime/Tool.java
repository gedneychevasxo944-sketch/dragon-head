package org.dragon.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Tool 接口 - 模型可调用的工具抽象。
 *
 * <p>对应 TypeScript 版本的 {@code src/Tool.ts} 中的 Tool 类型定义。
 * 每个工具需要实现以下核心方法：
 * <ul>
 *   <li>{@link #call} - 执行工具逻辑，返回结果</li>
 *   <li>{@link #mapToolResultToToolResultBlockParam} - 将工具输出转换为 API 格式</li>
 *   <li>schema 由 DB {@code tool_versions.parameters} 统一提供，通过 {@link org.dragon.tool.runtime.ToolDefinition} 传递</li>
 * </ul>
 *
 * <p>工具执行流程：
 * <pre>
 * 1. 模型发起 tool_use 请求
 * 2. 框架调用 validate(input, context) 校验参数
 * 3. 框架调用 checkPermissions(input, context) 检查权限
 * 4. 框架调用 call(input, context, progress) 执行工具
 * 5. 框架调用 mapToolResultToToolResultBlockParam(result) 转换结果
 * 6. 框架处理大结果持久化
 * 7. 框架构建 UserMessage 返回给模型
 * </pre>
 *
 * @param <I> 输入参数类型（通常为 JsonNode 或 POJO）
 * @param <O> 输出结果类型
 */
public interface Tool<I, O> {

    // ── 核心方法 ─────────────────────────────────────────────────────

    /**
     * 执行工具逻辑。
     *
     * <p>对应 TS: {@code tool.call(args, context, canUseTool, parentMessage, onProgress)}
     *
     * @param input    工具输入参数
     * @param context  执行上下文
     * @param progress 进度回调（可选）
     * @return 工具执行结果
     */
    CompletableFuture<ToolResult<O>> call(I input,
                                          ToolUseContext context,
                                          Consumer<ToolProgress> progress);

    /**
     * 将工具输出转换为 API 格式的 ToolResultBlockParam。
     *
     * <p>对应 TS: {@code tool.mapToolResultToToolResultBlockParam(content, toolUseID)}
     * 这是每个工具必须实现的方法，用于将工具特定的输出格式转换为
     * Claude API 能够理解的 {@link ToolResultBlockParam} 格式。
     *
     * <p>不同工具有不同的转换逻辑：
     * <ul>
     *   <li>BashTool: stdout/stderr → 文本内容</li>
     *   <li>FileReadTool: 文件内容 → 带行号的文本或图片块</li>
     *   <li>SkillTool: 根据 inline/fork 模式返回不同内容</li>
     * </ul>
     *
     * @param output    工具输出
     * @param toolUseId 工具调用 ID
     * @return API 格式的结果块
     */
    ToolResultBlockParam mapToolResultToToolResultBlockParam(O output, String toolUseId);

    // ── 元信息 ───────────────────────────────────────────────────────

    /**
     * 工具名称（模型通过此名称调用）。
     */
    String getName();

    /**
     * 工具描述（注入到模型的工具列表中）。
     */
    String getDescription();

    // ── 可选方法（有默认实现）───────────────────────────────────────

    /**
     * 校验输入参数。
     *
     * <p>对应 TS: {@code tool.validateInput(input, context)}
     * 在 call() 之前调用，用于参数格式校验。
     *
     * @param input   输入参数
     * @param context 执行上下文
     * @return 校验结果
     */
    default ValidationResult validateInput(I input, ToolUseContext context) {
        return ValidationResult.ok();
    }

    /**
     * 检查权限。
     *
     * <p>对应 TS: {@code tool.checkPermissions(input, context)}
     * 在 validateInput 通过后调用。
     *
     * @param input   输入参数
     * @param context 执行上下文
     * @return 权限检查结果
     */
    default PermissionResult checkPermissions(I input, ToolUseContext context) {
        return PermissionResult.allow();
    }

    /**
     * 工具是否为并发安全。
     *
     * <p>对应 TS: {@code tool.isConcurrencySafe(input)}
     * 用于判断是否可以并行执行多个工具调用。
     */
    default boolean isConcurrencySafe(I input) {
        return true;
    }

    /**
     * 工具是否为只读操作。
     *
     * <p>对应 TS: {@code tool.isReadOnly(input)}
     * 用于权限检查和 UI 显示。
     */
    default boolean isReadOnly(I input) {
        return true;
    }

    /**
     * 工具是否为破坏性操作。
     *
     * <p>对应 TS: {@code tool.isDestructive(input)}
     * 用于标记可能造成不可逆操作的工具（如删除、覆盖）。
     */
    default boolean isDestructive(I input) {
        return false;
    }

    /**
     * 中断行为。
     *
     * <p>对应 TS: {@code tool.interruptBehavior()}
     * 定义用户提交新消息时正在运行的工具的行为：
     * <ul>
     *   <li>CANCEL - 停止工具并丢弃结果</li>
     *   <li>BLOCK - 继续运行，新消息等待</li>
     * </ul>
     */
    default InterruptBehavior interruptBehavior() {
        return InterruptBehavior.BLOCK;
    }

    /**
     * 最大结果字符数。
     *
     * <p>对应 TS: {@code tool.maxResultSizeChars}
     * 超过此限制的结果会被持久化到磁盘，模型只收到预览。
     * 设置为 {@link Long#MAX_VALUE} 表示永不持久化（如 Read 工具）。
     */
    default long getMaxResultSizeChars() {
        return 50_000;
    }

    /**
     * 工具别名列表。
     *
     * <p>对应 TS: {@code tool.aliases}
     * 用于向后兼容，支持通过旧名称调用已重命名的工具。
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * 搜索提示。
     *
     * <p>对应 TS: {@code tool.searchHint}
     * 一句话描述工具能力，用于工具搜索的关键词匹配。
     */
    default String getSearchHint() {
        return null;
    }

    /**
     * 是否需要用户交互。
     *
     * <p>对应 TS: {@code tool.requiresUserInteraction()}
     * 用于判断工具是否可能弹出 UI 交互。
     */
    default boolean requiresUserInteraction() {
        return false;
    }

    /**
     * 是否已启用。
     *
     * <p>对应 TS: {@code tool.isEnabled()}
     */
    default boolean isEnabled() {
        return true;
    }

    // ── 输入处理 ─────────────────────────────────────────────────────

    /**
     * 解析输入参数。
     *
     * <p>将原始 JsonNode 转换为强类型输入对象。
     */
    default I parseInput(JsonNode input) {
        throw new UnsupportedOperationException("子类需实现 parseInput 方法");
    }

    /**
     * 回填可观察字段。
     *
     * <p>对应 TS: {@code tool.backfillObservableInput(input)}
     * 在观察者看到输入之前，补充遗留/派生字段。
     */
    default void backfillObservableInput(Map<String, Object> input) {
        // 默认无操作
    }

    // ── 渲染方法（UI 相关，可选）──────────────────────────────────────

    /**
     * 获取用户友好的工具名称。
     */
    default String getUserFacingName(I input) {
        return getName();
    }

    /**
     * 获取工具使用摘要（用于紧凑视图显示）。
     */
    default String getToolUseSummary(I input) {
        return null;
    }

    /**
     * 获取活动描述（用于加载状态显示）。
     *
     * <p>例如："Reading src/foo.ts", "Running tests"
     */
    default String getActivityDescription(I input) {
        return null;
    }

    // ── 内部类型 ─────────────────────────────────────────────────────

    /**
     * 中断行为枚举。
     */
    enum InterruptBehavior {
        CANCEL,
        BLOCK
    }
}

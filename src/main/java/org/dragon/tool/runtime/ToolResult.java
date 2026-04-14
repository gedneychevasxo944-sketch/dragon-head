package org.dragon.tool.runtime;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 工具执行结果。
 *
 * <p>对应 TypeScript 版本的 {@code ToolResult<T>} 类型定义：
 * <pre>
 * export type ToolResult&lt;T&gt; = {
 *   data: T
 *   newMessages?: Message[]
 *   contextModifier?: (context: ToolUseContext) =&gt; ToolUseContext
 * }
 * </pre>
 *
 * <p>结果处理链路：
 * <ol>
 *   <li>ATOMIC 工具：{@link Tool#mapToolResultToToolResultBlockParam(Object, String)}
 *       将 {@link #data} 转换为 {@link ToolResultBlockParam} 存入 {@link #resultBlock}</li>
 *   <li>HTTP/MCP/CODE 工具：Executor 直接构造文本 {@link ToolResultBlockParam} 存入 {@link #resultBlock}</li>
 *   <li>{@code ToolExecutionService.processResult()} 直接读取 {@link #resultBlock}，
 *       无需再猜测如何序列化 {@link #data}</li>
 * </ol>
 *
 * @param <T> 输出数据类型
 */
@Data
@Builder
public class ToolResult<T> {

    /**
     * 工具执行的主要输出数据（原始强类型结果）。
     *
     * <p>由 {@link Tool#mapToolResultToToolResultBlockParam(Object, String)} 转换为
     * API 格式，存入 {@link #resultBlock}。此字段保留供日志、存储、结构化输出使用。
     */
    private final T data;

    /**
     * 已转换好的 API 格式结果块。
     *
     * <p>由各 Executor 在执行完成后填充：
     * <ul>
     *   <li>ATOMIC：调用 {@link Tool#mapToolResultToToolResultBlockParam} 生成</li>
     *   <li>HTTP/MCP/CODE/SKILL：直接构造 {@link ToolResultBlockParam#ofText} 或 ofBlocks</li>
     * </ul>
     *
     * <p>{@code ToolExecutionService.processResult()} 优先使用此字段；
     * 若为 null（理论上不应发生），则 fallback 到序列化 {@link #data}。
     */
    private final ToolResultBlockParam resultBlock;

    /**
     * 需要注入对话的新消息列表。
     *
     * <p>对应 TS: {@code newMessages}
     * 某些工具执行后需要向对话添加额外消息（如 Skill 的 inline 模式）。
     */
    private final List<Map<String, Object>> newMessages;

    /**
     * 上下文修改器。
     *
     * <p>对应 TS: {@code contextModifier}
     * 用于修改后续工具执行的上下文（如临时添加工具权限）。
     * 仅对非并发安全的工具有效。
     */
    private final Function<ToolUseContext, ToolUseContext> contextModifier;

    /**
     * 是否成功。
     */
    private final boolean success;

    /**
     * 错误信息（失败时）。
     */
    private final String error;

    // ── 静态工厂方法 ─────────────────────────────────────────────────────

    /**
     * 创建成功结果（不含 resultBlock，用于工具内部返回，由 Executor 后续调用
     * {@link Tool#mapToolResultToToolResultBlockParam} 填充 resultBlock）。
     */
    public static <T> ToolResult<T> ok(T data) {
        return ToolResult.<T>builder()
                .data(data)
                .success(true)
                .build();
    }

    /**
     * 创建成功结果（带 resultBlock）。
     *
     * <p>由 Executor 在完成 {@link Tool#mapToolResultToToolResultBlockParam} 转换后调用。
     */
    public static <T> ToolResult<T> ok(T data, ToolResultBlockParam resultBlock) {
        return ToolResult.<T>builder()
                .data(data)
                .resultBlock(resultBlock)
                .success(true)
                .build();
    }

    /**
     * 创建成功结果（带新消息）。
     */
    public static <T> ToolResult<T> ok(T data, List<Map<String, Object>> newMessages) {
        return ToolResult.<T>builder()
                .data(data)
                .newMessages(newMessages)
                .success(true)
                .build();
    }

    /**
     * 创建成功结果（带新消息 + resultBlock）。
     */
    public static <T> ToolResult<T> ok(T data,
                                        ToolResultBlockParam resultBlock,
                                        List<Map<String, Object>> newMessages) {
        return ToolResult.<T>builder()
                .data(data)
                .resultBlock(resultBlock)
                .newMessages(newMessages)
                .success(true)
                .build();
    }

    /**
     * 创建成功结果（带 contextModifier）。
     *
     * <p>contextModifier 用于修改后续工具执行的上下文（如 Skill 临时扬权）。
     */
    public static <T> ToolResult<T> ok(T data,
                                        ToolResultBlockParam resultBlock,
                                        List<Map<String, Object>> newMessages,
                                        Function<ToolUseContext, ToolUseContext> contextModifier) {
        return ToolResult.<T>builder()
                .data(data)
                .resultBlock(resultBlock)
                .newMessages(newMessages)
                .contextModifier(contextModifier)
                .success(true)
                .build();
    }

    /**
     * 创建失败结果。
     */
    public static <T> ToolResult<T> fail(String error) {
        return ToolResult.<T>builder()
                .success(false)
                .error(error)
                .build();
    }

    /**
     * 创建失败结果（带数据）。
     */
    public static <T> ToolResult<T> fail(String error, T partialData) {
        return ToolResult.<T>builder()
                .data(partialData)
                .success(false)
                .error(error)
                .build();
    }

}

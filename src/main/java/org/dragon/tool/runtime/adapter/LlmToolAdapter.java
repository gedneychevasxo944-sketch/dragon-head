package org.dragon.tool.runtime.adapter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * LLM 工具适配器接口。
 *
 * <p>负责平台内部统一格式（{@link UnifiedToolDeclaration} / {@link ToolCallRequest}）
 * 与各 LLM 厂商格式之间的双向转换。每个厂商实现一个适配器，通过
 * {@link LlmAdapterRegistry} 按厂商标识注册和查找。
 *
 * <p><b>转换方向</b>：
 * <pre>
 * 注入工具到 LLM：
 *   UnifiedToolDeclaration  →  toProviderFormat()  →  LLM 能理解的工具声明 JSON
 *
 * 解析 LLM 返回的 tool_call：
 *   LLM raw tool_call JSON  →  parseToolCall()  →  ToolCallRequest（统一格式）
 *
 * 构造返回给 LLM 的 tool_result：
 *   ToolResult content      →  toToolResultFormat()  →  LLM 能处理的 tool_result 消息 JSON
 * </pre>
 *
 * <p><b>已有实现</b>：
 * <ul>
 *   <li>{@link AnthropicToolAdapter} — Anthropic Claude（input_schema / tool_use）</li>
 *   <li>{@link OpenAiToolAdapter}    — OpenAI（parameters / tool_calls）</li>
 * </ul>
 */
public interface LlmToolAdapter {

    /**
     * 返回此适配器支持的 LLM 厂商标识。
     * 与 {@link ToolCallRequest#getLlmProvider()} 对应，用于在
     * {@link LlmAdapterRegistry} 中注册和查找。
     *
     * @return 厂商标识，如 {@code "anthropic"}、{@code "openai"}
     */
    String supportedProvider();

    /**
     * 将平台内部统一工具声明格式转换为该 LLM 厂商的工具声明格式。
     *
     * <p>转换结果将填入 LLM API 请求的 {@code tools} 数组字段，告知 LLM 可用的工具列表。
     *
     * @param declaration 平台内部统一工具声明
     * @return 该 LLM 厂商格式的工具声明 JSON
     */
    JsonNode toProviderFormat(UnifiedToolDeclaration declaration);

    /**
     * 批量转换工具声明列表（便捷方法，默认逐个调用 {@link #toProviderFormat}）。
     *
     * @param declarations 工具声明列表
     * @return 转换后的工具声明 JSON 列表，顺序与输入一致
     */
    default List<JsonNode> toProviderFormatList(List<UnifiedToolDeclaration> declarations) {
        return declarations.stream()
                .map(this::toProviderFormat)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 将该 LLM 厂商返回的 tool_call 原始 JSON 解析为平台统一格式。
     *
     * <p><b>注意</b>：
     * <ul>
     *   <li>Anthropic 的 input 字段是 JSON 对象，可直接使用</li>
     *   <li>OpenAI 的 arguments 字段是 JSON 字符串，需先用 ObjectMapper 解析</li>
     * </ul>
     *
     * @param rawToolCall LLM 返回的 tool_call 原始 JSON（单个调用块）
     * @return 解析后的统一格式 ToolCallRequest
     * @throws IllegalArgumentException 如果 rawToolCall 格式不符合该厂商规范
     */
    ToolCallRequest parseToolCall(JsonNode rawToolCall);

    /**
     * 将工具执行结果转换为该 LLM 厂商期望的 tool_result 消息格式。
     *
     * <p>转换结果将作为下一轮 LLM API 请求中的 user 消息内容（tool_result 块），
     * 告知 LLM 工具调用的执行结果。
     *
     * @param toolCallId 工具调用 ID（与 {@link ToolCallRequest#getToolCallId()} 对应）
     * @param content    工具执行结果文本内容
     * @param isError    是否为错误结果（true 时 LLM 会知道工具执行失败）
     * @return 该 LLM 厂商格式的 tool_result 消息 JSON
     */
    JsonNode toToolResultFormat(String toolCallId, String content, boolean isError);
}

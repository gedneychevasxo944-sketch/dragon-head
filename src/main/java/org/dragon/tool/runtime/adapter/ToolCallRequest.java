package org.dragon.tool.runtime.adapter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * LLM 返回的 tool_call 解析后的统一格式。
 *
 * <p>各 LLM 厂商的 tool_call 结构各不相同，通过 {@link LlmToolAdapter#parseToolCall(JsonNode)}
 * 统一解析为此格式，供 {@code ToolDispatcher} 进行路由执行。
 *
 * <p><b>Anthropic 原始格式</b>（直接 JSON 对象参数）：
 * <pre>
 * {"type":"tool_use","id":"toolu_xxx","name":"bash","input":{"command":"ls -la"}}
 * </pre>
 *
 * <p><b>OpenAI 原始格式</b>（arguments 是 JSON 字符串，需二次解析）：
 * <pre>
 * {"tool_calls":[{"id":"call_xxx","type":"function",
 *   "function":{"name":"bash","arguments":"{\"command\":\"ls -la\"}"}}]}
 * </pre>
 *
 * <p>两者解析后均为：
 * <pre>
 * ToolCallRequest {
 *   toolCallId  = "toolu_xxx" / "call_xxx"
 *   toolName    = "bash"
 *   parameters  = {"command": "ls -la"}  (JsonNode 对象)
 *   llmProvider = "anthropic" / "openai"
 * }
 * </pre>
 */
public class ToolCallRequest {

    /**
     * LLM 分配的工具调用 ID，用于构造 tool_result 消息返回给 LLM。
     * Anthropic：{@code toolu_xxx}；OpenAI：{@code call_xxx}。
     */
    private String toolCallId;

    /**
     * 工具名称，与 {@link UnifiedToolDeclaration#getName()} 对应。
     * ToolDispatcher 通过此名称在 ToolRegistry 中查找工具。
     */
    private String toolName;

    /**
     * 工具调用参数（已解析为 JsonNode 对象）。
     * Anthropic 原始 input 字段直接使用；OpenAI arguments 字符串需先解析为 JSON 对象。
     * 各 ToolExecutor 从此字段中按参数名读取具体值。
     */
    private JsonNode parameters;

    /**
     * 来源 LLM 厂商标识，如 {@code "anthropic"}、{@code "openai"}。
     * 用于在构造 tool_result 时找到对应的 {@link LlmToolAdapter} 进行格式转换。
     */
    private String llmProvider;

    public ToolCallRequest() {
    }

    public ToolCallRequest(String toolCallId, String toolName, JsonNode parameters, String llmProvider) {
        this.toolCallId = Objects.requireNonNull(toolCallId, "toolCallId must not be null");
        this.toolName = Objects.requireNonNull(toolName, "toolName must not be null");
        this.parameters = parameters;
        this.llmProvider = Objects.requireNonNull(llmProvider, "llmProvider must not be null");
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public JsonNode getParameters() {
        return parameters;
    }

    public void setParameters(JsonNode parameters) {
        this.parameters = parameters;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    @Override
    public String toString() {
        return "ToolCallRequest{toolCallId='" + toolCallId + "', toolName='" + toolName
                + "', llmProvider='" + llmProvider + "'}";
    }
}

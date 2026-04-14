package org.dragon.tool.runtime.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * OpenAI 工具适配器。
 *
 * <p>实现 {@link LlmToolAdapter}，负责平台内部格式与 OpenAI Chat Completions API 格式的双向转换。
 *
 * <p><b>工具声明格式（注入 LLM）</b>：
 * <pre>
 * {
 *   "type": "function",
 *   "function": {
 *     "name": "bash",
 *     "description": "Run a bash command",
 *     "parameters": {
 *       "type": "object",
 *       "properties": {
 *         "command": {"type": "string", "description": "The bash command to run"}
 *       },
 *       "required": ["command"]
 *     }
 *   }
 * }
 * </pre>
 *
 * <p><b>LLM 返回的 tool_call 格式（choices[0].message.tool_calls 数组元素）</b>：
 * <pre>
 * {
 *   "id": "call_xxx",
 *   "type": "function",
 *   "function": {"name": "bash", "arguments": "{\"command\":\"ls -la\"}"}
 * }
 * </pre>
 * 注意：{@code arguments} 是 JSON 字符串而非 JSON 对象，需要二次解析。
 *
 * <p><b>tool_result 消息格式</b>（role=tool 的 message）：
 * <pre>
 * {"role": "tool", "tool_call_id": "call_xxx", "content": "..."}
 * </pre>
 * OpenAI 没有 is_error 字段，错误信息直接写在 content 中。
 */
public class OpenAiToolAdapter implements LlmToolAdapter {

    /** OpenAI 厂商标识 */
    public static final String PROVIDER = "openai";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String supportedProvider() {
        return PROVIDER;
    }

    /**
     * 将 {@link UnifiedToolDeclaration} 转换为 OpenAI function tool 声明格式。
     *
     * <p>OpenAI 使用 {@code parameters}（JSON Schema）描述参数，
     * 外层包裹 {@code {"type":"function","function":{...}}}。
     */
    @Override
    public JsonNode toProviderFormat(UnifiedToolDeclaration declaration) {
        ObjectNode tool = MAPPER.createObjectNode();
        tool.put("type", "function");

        ObjectNode function = MAPPER.createObjectNode();
        function.put("name", declaration.getName());
        function.put("description", declaration.getDescription());

        // 构造 parameters（JSON Schema 格式，与 Anthropic input_schema 结构相同）
        ObjectNode parameters = MAPPER.createObjectNode();
        parameters.put("type", "object");

        ObjectNode properties = MAPPER.createObjectNode();
        if (declaration.getParameters() != null) {
            for (Map.Entry<String, ParameterSchema> entry : declaration.getParameters().entrySet()) {
                properties.set(entry.getKey(), buildPropertyNode(entry.getValue()));
            }
        }
        parameters.set("properties", properties);

        if (declaration.getRequired() != null && !declaration.getRequired().isEmpty()) {
            ArrayNode requiredArray = MAPPER.createArrayNode();
            declaration.getRequired().forEach(requiredArray::add);
            parameters.set("required", requiredArray);
        }

        function.set("parameters", parameters);
        tool.set("function", function);
        return tool;
    }

    /**
     * 将 OpenAI tool_calls 数组中的单个元素解析为 {@link ToolCallRequest}。
     *
     * <p>OpenAI tool_call 格式：
     * {@code {"id":"call_xxx","type":"function","function":{"name":"bash","arguments":"{...}"}}}
     *
     * <p>注意 {@code arguments} 是 JSON 字符串，需要用 ObjectMapper 二次解析为 JSON 对象。
     */
    @Override
    public ToolCallRequest parseToolCall(JsonNode rawToolCall) {
        String type = rawToolCall.path("type").asText();
        if (!"function".equals(type)) {
            throw new IllegalArgumentException(
                    "Expected OpenAI function tool_call, got type: " + type);
        }
        String toolCallId = rawToolCall.path("id").asText();
        JsonNode functionNode = rawToolCall.path("function");
        String toolName = functionNode.path("name").asText();

        // arguments 是 JSON 字符串，需要二次解析为 JsonNode 对象
        String argumentsStr = functionNode.path("arguments").asText();
        JsonNode parameters;
        try {
            parameters = MAPPER.readTree(argumentsStr);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse OpenAI tool_call arguments as JSON: " + argumentsStr, e);
        }

        return new ToolCallRequest(toolCallId, toolName, parameters, PROVIDER);
    }

    /**
     * 将工具执行结果转换为 OpenAI tool message 格式。
     *
     * <p>OpenAI tool_result 格式（role=tool 的独立 message）：
     * <pre>
     * {"role": "tool", "tool_call_id": "call_xxx", "content": "..."}
     * </pre>
     * OpenAI 没有显式的 is_error 字段，错误时在 content 前加 "Error: " 前缀。
     */
    @Override
    public JsonNode toToolResultFormat(String toolCallId, String content, boolean isError) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("role", "tool");
        result.put("tool_call_id", toolCallId);
        String finalContent = isError
                ? "Error: " + (content != null ? content : "")
                : (content != null ? content : "");
        result.put("content", finalContent);
        return result;
    }

    // ── 内部辅助方法 ──────────────────────────────────────────────────

    /**
     * 将 {@link ParameterSchema} 递归转换为 JSON Schema property 节点。
     * 逻辑与 {@link AnthropicToolAdapter} 完全相同（JSON Schema 规范一致）。
     */
    private ObjectNode buildPropertyNode(ParameterSchema schema) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", schema.getType());
        if (schema.getDescription() != null) {
            node.put("description", schema.getDescription());
        }

        if (schema.getEnumValues() != null && !schema.getEnumValues().isEmpty()) {
            ArrayNode enumArray = MAPPER.createArrayNode();
            schema.getEnumValues().forEach(enumArray::add);
            node.set("enum", enumArray);
        }

        if ("array".equals(schema.getType()) && schema.getItems() != null) {
            node.set("items", buildPropertyNode(schema.getItems()));
        }

        if ("object".equals(schema.getType()) && schema.getProperties() != null) {
            ObjectNode props = MAPPER.createObjectNode();
            for (Map.Entry<String, ParameterSchema> entry : schema.getProperties().entrySet()) {
                props.set(entry.getKey(), buildPropertyNode(entry.getValue()));
            }
            node.set("properties", props);
            if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                ArrayNode reqArr = MAPPER.createArrayNode();
                schema.getRequired().forEach(reqArr::add);
                node.set("required", reqArr);
            }
        }

        return node;
    }
}

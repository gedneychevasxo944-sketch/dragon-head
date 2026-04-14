package org.dragon.tool.runtime.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Anthropic Claude 工具适配器。
 *
 * <p>实现 {@link LlmToolAdapter}，负责平台内部格式与 Anthropic Claude API 格式的双向转换。
 *
 * <p><b>工具声明格式（注入 LLM）</b>：
 * <pre>
 * {
 *   "name": "bash",
 *   "description": "Run a bash command",
 *   "input_schema": {
 *     "type": "object",
 *     "properties": {
 *       "command": {"type": "string", "description": "The bash command to run"}
 *     },
 *     "required": ["command"]
 *   }
 * }
 * </pre>
 *
 * <p><b>LLM 返回的 tool_call 格式（单个 content block）</b>：
 * <pre>
 * {"type": "tool_use", "id": "toolu_xxx", "name": "bash", "input": {"command": "ls -la"}}
 * </pre>
 * input 字段直接是 JSON 对象，无需二次解析。
 *
 * <p><b>tool_result 消息格式</b>：
 * <pre>
 * {"type": "tool_result", "tool_use_id": "toolu_xxx", "content": "...", "is_error": false}
 * </pre>
 */
public class AnthropicToolAdapter implements LlmToolAdapter {

    /** Anthropic 厂商标识 */
    public static final String PROVIDER = "anthropic";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String supportedProvider() {
        return PROVIDER;
    }

    /**
     * 将 {@link UnifiedToolDeclaration} 转换为 Anthropic Claude 工具声明格式。
     *
     * <p>Anthropic 使用 {@code input_schema}（JSON Schema）描述参数，
     * 顶层是 {@code {"type":"object","properties":{...},"required":[...]}}。
     */
    @Override
    public JsonNode toProviderFormat(UnifiedToolDeclaration declaration) {
        ObjectNode tool = MAPPER.createObjectNode();
        tool.put("name", declaration.getName());
        tool.put("description", declaration.getDescription());

        // 构造 input_schema（JSON Schema 格式）
        ObjectNode inputSchema = MAPPER.createObjectNode();
        inputSchema.put("type", "object");

        // 构造 properties
        ObjectNode properties = MAPPER.createObjectNode();
        if (declaration.getParameters() != null) {
            for (Map.Entry<String, ParameterSchema> entry : declaration.getParameters().entrySet()) {
                properties.set(entry.getKey(), buildPropertyNode(entry.getValue()));
            }
        }
        inputSchema.set("properties", properties);

        // 构造 required 数组
        if (declaration.getRequired() != null && !declaration.getRequired().isEmpty()) {
            ArrayNode requiredArray = MAPPER.createArrayNode();
            declaration.getRequired().forEach(requiredArray::add);
            inputSchema.set("required", requiredArray);
        }

        tool.set("input_schema", inputSchema);
        return tool;
    }

    /**
     * 将 Anthropic 返回的 tool_use content block 解析为 {@link ToolCallRequest}。
     *
     * <p>Anthropic tool_use block 格式：
     * {@code {"type":"tool_use","id":"toolu_xxx","name":"bash","input":{...}}}
     * input 字段直接是 JSON 对象，无需二次解析。
     */
    @Override
    public ToolCallRequest parseToolCall(JsonNode rawToolCall) {
        String type = rawToolCall.path("type").asText();
        if (!"tool_use".equals(type)) {
            throw new IllegalArgumentException(
                    "Expected Anthropic tool_use block, got type: " + type);
        }
        String toolCallId = rawToolCall.path("id").asText();
        String toolName = rawToolCall.path("name").asText();
        JsonNode parameters = rawToolCall.path("input");

        return new ToolCallRequest(toolCallId, toolName, parameters, PROVIDER);
    }

    /**
     * 将工具执行结果转换为 Anthropic tool_result 消息格式。
     *
     * <p>Anthropic tool_result 格式（作为 user message content block）：
     * <pre>
     * {"type": "tool_result", "tool_use_id": "toolu_xxx", "content": "...", "is_error": false}
     * </pre>
     */
    @Override
    public JsonNode toToolResultFormat(String toolCallId, String content, boolean isError) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("type", "tool_result");
        result.put("tool_use_id", toolCallId);
        result.put("content", content != null ? content : "");
        result.put("is_error", isError);
        return result;
    }

    // ── 内部辅助方法 ──────────────────────────────────────────────────

    /**
     * 将 {@link ParameterSchema} 递归转换为 JSON Schema property 节点。
     */
    private ObjectNode buildPropertyNode(ParameterSchema schema) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", schema.getType());
        if (schema.getDescription() != null) {
            node.put("description", schema.getDescription());
        }

        // 枚举值
        if (schema.getEnumValues() != null && !schema.getEnumValues().isEmpty()) {
            ArrayNode enumArray = MAPPER.createArrayNode();
            schema.getEnumValues().forEach(enumArray::add);
            node.set("enum", enumArray);
        }

        // array 类型的 items
        if ("array".equals(schema.getType()) && schema.getItems() != null) {
            node.set("items", buildPropertyNode(schema.getItems()));
        }

        // object 类型的 properties 和 required
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

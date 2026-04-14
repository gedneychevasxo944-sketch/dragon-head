package org.dragon.tool.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.tool.runtime.adapter.ParameterSchema;
import org.dragon.tool.runtime.adapter.UnifiedToolDeclaration;

import java.util.Objects;

/**
 * MCP 协议返回的工具描述（tools/list 响应中的单个工具元素）。
 *
 * <p>对应 MCP 规范中 {@code tools/list} 响应的 {@code tools} 数组元素结构：
 * <pre>
 * {
 *   "name": "search_code",
 *   "description": "Search for code in the repository",
 *   "inputSchema": {
 *     "type": "object",
 *     "properties": {
 *       "query": {"type": "string", "description": "Search query"}
 *     },
 *     "required": ["query"]
 *   }
 * }
 * </pre>
 *
 * <p>{@link McpToolLoader} 将此结构转换为平台内部的 {@link UnifiedToolDeclaration}
 * 存入 {@link org.dragon.tool.domain.ToolVersionDO#getDeclaration()}。
 *
 * <p>{@link #inputSchema} 直接保存为 JsonNode，
 * 其 {@code properties} 结构与 {@link ParameterSchema} 一一对应，
 * 在转换时按 JSON Schema 规范递归映射。
 */
public class McpToolDefinition {

    /** MCP Server 返回的原始工具名（未包含 server 前缀），如 {@code search_code} */
    private String name;

    /** 工具描述 */
    private String description;

    /**
     * 工具参数的 JSON Schema（来自 MCP 协议，直接存储原始 JsonNode）。
     * 结构为标准 JSON Schema：
     * <pre>
     * {
     *   "type": "object",
     *   "properties": { ... },
     *   "required": [ ... ]
     * }
     * </pre>
     */
    private JsonNode inputSchema;

    public McpToolDefinition() {
    }

    public McpToolDefinition(String name, String description, JsonNode inputSchema) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(JsonNode inputSchema) {
        this.inputSchema = inputSchema;
    }

    @Override
    public String toString() {
        return "McpToolDefinition{name='" + name + "', description='" + description + "'}";
    }
}

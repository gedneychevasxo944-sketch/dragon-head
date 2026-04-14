package org.dragon.tool.runtime.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Map;

/**
 * 统一工具声明格式（平台内部中间格式）。
 *
 * <p>这是平台内部用于描述工具对外接口的统一结构，独立于任何 LLM 厂商的格式。
 * 通过 {@link LlmToolAdapter} 的实现类将此格式转换为各厂商格式：
 * <ul>
 *   <li>{@link AnthropicToolAdapter}：转换为 Anthropic Claude 的 tool 格式（input_schema）</li>
 *   <li>{@link OpenAiToolAdapter}：转换为 OpenAI 的 function 格式（parameters）</li>
 * </ul>
 *
 * <p>实例由 {@link ToolDeclarationBuilder} 从 {@link org.dragon.tool.domain.ToolVersionDO}
 * 的声明字段（toolName / toolDescription / parameters / requiredParams / aliases）构建。
 *
 * <p><b>对应 TS 项目 src/Tool.ts 中</b>：
 * <pre>
 *   name           → tool.name
 *   description    → tool.description()
 *   parameters     → tool.inputSchema（Zod schema 的 JSON Schema 序列化）
 *   required       → inputSchema.required
 *   aliases        → tool.aliases
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedToolDeclaration {

    /**
     * 工具名称，LLM 通过此名称发起 tool_call。
     * MCP 工具名格式：{@code mcp__{serverName}__{toolName}}
     */
    private String name;

    /**
     * 工具描述，注入到 LLM system prompt 的工具列表中，
     * 帮助 LLM 理解何时调用该工具及其能力范围。
     */
    private String description;

    /**
     * 工具参数的 Schema Map。
     * key 为参数名，value 为参数的 {@link ParameterSchema}（类型、描述、枚举值等）。
     * 整体结构对应 JSON Schema 中 properties 字段。
     */
    @Singular("parameter")
    private Map<String, ParameterSchema> parameters;

    /**
     * 必填参数名列表。
     * 此列表中的参数名必须出现在 {@link #parameters} 中。
     */
    @Singular("requiredParam")
    private List<String> required;

    /**
     * 工具别名列表（可选）。
     * 用于向后兼容，支持通过旧名称调用已重命名的工具。
     * 对应 TS 项目 {@code tool.aliases}。
     */
    @Singular("alias")
    private List<String> aliases;
}

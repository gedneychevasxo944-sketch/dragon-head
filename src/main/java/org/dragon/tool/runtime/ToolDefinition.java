package org.dragon.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.tool.enums.ToolType;

import java.util.List;

/**
 * Tool 执行定义（内存对象，非 DB 实体）。
 *
 * <p>由 {@link ToolRegistry} 从 DB 加载后聚合，供 {@link ToolFactory}、{@link ToolRegistry}
 * 及各 {@link Tool} 实现类使用。
 *
 * <p>仅包含 Tool 执行所需的信息，不含管理端元数据
 * （如 creatorId、createdAt、publishedAt、releaseNote、status 等）。
 *
 * <p>字段分类：
 * <ul>
 *   <li><b>标识</b>：toolId、toolType、version</li>
 *   <li><b>LLM 声明</b>：name、description、parameters、requiredParams、aliases——
 *       注入 LLM system prompt 工具列表，由 {@link org.dragon.tool.runtime.adapter.ToolDeclarationBuilder}
 *       解析为 {@link org.dragon.tool.runtime.adapter.UnifiedToolDeclaration}</li>
 *   <li><b>执行配置</b>：executionConfig——各 ToolType 专有配置，由对应 Factory 解析</li>
 *   <li><b>存储信息</b>：storageType、storageInfo——CODE/SKILL 类型工具关联文件的存储位置</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDefinition {

    // ── 标识 ─────────────────────────────────────────────────────

    /** 所属工具 ID */
    private String toolId;

    /** 工具类型（路由到对应 ToolFactory） */
    private ToolType toolType;

    /** 当前有效版本号 */
    private int version;

    // ── LLM 声明字段（注入 system prompt 工具列表） ──────────────────

    /**
     * 工具名称，LLM 通过此名称发起 tool_call。
     * MCP 工具名格式：{@code mcp__{serverName}__{toolName}}。
     */
    private String name;

    /**
     * 工具描述，注入到 LLM system prompt 的工具列表中，
     * 帮助 LLM 理解何时调用该工具及其能力范围。
     */
    private String description;

    /**
     * 工具参数 Schema（JSON 格式）。
     * 结构为 {@code Map<String, ParameterSchema>}，对应 JSON Schema 的 properties 字段。
     * 由 {@link org.dragon.tool.runtime.adapter.ToolDeclarationBuilder} 反序列化后构建
     * {@link org.dragon.tool.runtime.adapter.UnifiedToolDeclaration}。
     */
    private String parameters;

    /**
     * 必填参数名列表（JSON 数组格式，如 {@code ["command","path"]}）。
     * 列表中的参数名必须出现在 {@link #parameters} 中。
     */
    private String requiredParams;

    /**
     * 工具别名列表（反序列化后的 Java List，可为 null）。
     * 用于向后兼容，支持通过旧名称调用已重命名的工具。
     */
    private List<String> aliases;

    // ── 执行配置 ──────────────────────────────────────────────────

    /**
     * 执行配置（各 ToolType 专有配置，由对应 Factory 解析）。
     *
     * <p>各类型示例：
     * <pre>
     * ATOMIC:    {"className": "org.dragon.tool.runtime.tools.BashTool"}
     * HTTP:      {"url": "https://...", "method": "POST", "headers": {...}, "bodyTemplate": "..."}
     * MCP:       {"serverName": "github", "mcpToolName": "search_code"}
     * CODE:      {"language": "python", "scriptContent": "...", "entrypoint": "main"}
     * SKILL:     {"skillName": "code-review"}
     * </pre>
     */
    private JsonNode executionConfig;

    // ── 存储信息（CODE/SKILL 类型工具关联文件） ──────────────────────

    /**
     * 文件存储类型标识（标识 {@link #storageInfo} 中路径的解析方式）。
     * 未上传任何文件时为 null。
     */
    private String storageType;

    /**
     * 文件存储元信息（JSON 格式）。
     * 记录该版本上传的所有可执行脚本或辅助文件的位置信息。
     * 未上传任何文件时为 null。
     */
    private String storageInfo;

    // ── 标签（用于工具过滤/展示） ──────────────────────────────────

    /** 标签列表 */
    private List<String> tags;
}


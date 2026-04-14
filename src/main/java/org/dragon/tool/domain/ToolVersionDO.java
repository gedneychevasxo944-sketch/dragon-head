package org.dragon.tool.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVersionStatus;

import java.time.LocalDateTime;

/**
 * 工具版本领域对象。
 *
 * <p>每次更新工具内容时 INSERT 新记录，toolId 不变，version +1。
 *
 * <p>字段分类：
 * <ul>
 *   <li>标识：id, toolId, version</li>
 *   <li>LLM 声明：toolName, toolDescription, parameters, requiredParams, aliases</li>
 *   <li>执行配置：executionConfig</li>
 *   <li>冗余字段：toolType（便于不 join ToolDO 直接路由执行）</li>
 *   <li>编辑者：editorId, editorName</li>
 *   <li>版本状态：status, releaseNote</li>
 *   <li>时间戳：createdAt, publishedAt</li>
 * </ul>
 *
 * <p><b>LLM 声明字段说明</b>：
 * <ul>
 *   <li>{@link #name}        - 工具名称，LLM 通过此名称发起 tool_call</li>
 *   <li>{@link #description} - 工具描述，注入 LLM system prompt，帮助 LLM 理解何时调用该工具</li>
 *   <li>{@link #parameters}      - 参数 Schema（JSON 格式，Map&lt;String, ParameterSchema&gt;），
 *                                   对应 JSON Schema 的 properties</li>
 *   <li>{@link #requiredParams}  - 必填参数名列表（JSON 数组格式，如 {@code ["command","path"]}）</li>
 *   <li>{@link #aliases}         - 工具别名列表（JSON 数组格式，向后兼容旧名称，可为 null）</li>
 * </ul>
 *
 * <p><b>executionConfig 各类型示例</b>：
 * <pre>
 * ATOMIC:    {"className": "org.dragon.tool.impl.BashTool"}
 * HTTP:      {"url": "https://...", "method": "POST", "headers": {...}, "bodyTemplate": "..."}
 * MCP:       {"serverName": "github", "mcpToolName": "search_code"}
 * CODE:      {"language": "python", "scriptContent": "...", "entrypoint": "main"}
 * SKILL:     {"skillName": "code-review"}
 * </pre>
 */
@Data
public class ToolVersionDO {

    // ── 标识 ─────────────────────────────────────────────────────

    /** 物理自增主键 */
    private Long id;

    /** 所属工具 ID，关联 {@link ToolDO#getId()} */
    private String toolId;

    /** 版本号（整数递增，从 1 开始） */
    private Integer version;

    // ── LLM 声明字段 ─────────────────────────────────────────────

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
     * 由 {@code ToolDeclarationBuilder} 反序列化后构建 {@code UnifiedToolDeclaration}。
     */
    private String parameters;

    /**
     * 必填参数名列表（JSON 数组格式，如 {@code ["command","path"]}）。
     * 列表中的参数名必须出现在 {@link #parameters} 中。
     */
    private String requiredParams;

    /**
     * 工具别名列表（JSON 数组格式，可为 null）。
     * 用于向后兼容，支持通过旧名称调用已重命名的工具。
     */
    private String aliases;

    // ── 执行配置 ─────────────────────────────────────────────────

    /**
     * 执行配置（JSON 格式）。
     * 内容随 ToolType 不同，由对应 ToolExecutor 解析，平台层不解析此字段。
     */
    private JsonNode executionConfig;

    // ── 冗余字段 ─────────────────────────────────────────────────

    /** 该版本的工具类型（冗余字段，便于不 join ToolDO 直接路由执行） */
    private ToolType toolType;

    // ── 编辑者 ─────────────────────────────────────────────────

    /** 本次编辑者用户 ID */
    private Long editorId;

    /** 本次编辑者用户名 */
    private String editorName;

    // ── 存储信息 ─────────────────────────────────────────────────

    /**
     * 文件存储元信息（JSON 格式，映射 {@link ToolStorageInfoVO}）。
     *
     * <p>记录该版本上传的所有可执行脚本或辅助文件的位置信息，storageType 内聚在 VO 的 type 字段中。
     * 未上传任何文件时为 null。
     * 反序列化后可获得 type、basePath、bucket 及文件列表，供执行时加载文件使用。
     */
    private String storageInfo;

    // ── 版本状态 ─────────────────────────────────────────────────

    /** 版本状态 */
    private ToolVersionStatus status;

    /** 发版备注 */
    private String releaseNote;

    // ── 时间戳 ─────────────────────────────────────────────────

    /** 版本创建时间 */
    private LocalDateTime createdAt;

    /** 发布时间 */
    private LocalDateTime publishedAt;
}

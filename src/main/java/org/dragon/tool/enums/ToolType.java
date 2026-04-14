package org.dragon.tool.enums;

/**
 * 工具类型枚举。
 *
 * <ul>
 *   <li>{@link #ATOMIC}    - 原子工具：Java 代码内建，直接由平台执行（如 BashTool、FileReadTool）</li>
 *   <li>{@link #HTTP}      - HTTP 工具：调用固定的第三方 HTTP 接口，参数/响应由平台映射</li>
 *   <li>{@link #MCP}       - MCP 工具：通过 Model Context Protocol 连接外部 MCP Server 动态发现，
 *                            工具名格式为 {@code mcp__{serverName}__{toolName}}</li>
 *   <li>{@link #CODE}      - 代码工具：用户上传的脚本（Python/Shell 等），由沙箱环境执行（沙箱预留接口）</li>
 *   <li>{@link #AGENT}     - Agent 工具：调用另一个 Agent 子任务，参数为 goal/context（预留）</li>
 *   <li>{@link #SKILL}     - Skill 工具：桥接 Skill 系统，通过 SkillToolExecutor 将 skill.md 注入上下文</li>
 *   <li>{@link #COMPOSITE} - 组合工具：由多个子工具按顺序/并行组合而成的宏工具（预留）</li>
 * </ul>
 */
public enum ToolType {

    /**
     * 原子工具：Java 代码内建。
     * executionConfig 示例：{"className": "org.dragon.tool.impl.BashTool"}
     */
    ATOMIC,

    /**
     * HTTP 工具：调用第三方 HTTP 接口。
     * executionConfig 示例：{"url":"https://...","method":"POST","headers":{...},"bodyTemplate":"..."}
     */
    HTTP,

    /**
     * MCP 工具：通过 MCP 协议连接外部 Server 动态发现。
     * 工具名格式：mcp__{serverName}__{toolName}
     * executionConfig 示例：{"serverName":"github","mcpToolName":"search_code"}
     */
    MCP,

    /**
     * 代码工具：用户上传的脚本。
     * executionConfig 示例：{"language":"python","scriptContent":"...","entrypoint":"main"}
     */
    CODE,

    /**
     * Agent 工具：调用子 Agent 任务（预留）。
     * executionConfig 示例：{"agentDefinition":"..."}
     */
    AGENT,

    /**
     * Skill 工具：桥接 Skill 系统。
     * executionConfig 示例：{"skillName":"code-review"}
     */
    SKILL,

    /**
     * 组合工具：多个子工具的组合宏工具（预留）。
     * executionConfig 示例：{"steps":[...]}
     */
    COMPOSITE
}

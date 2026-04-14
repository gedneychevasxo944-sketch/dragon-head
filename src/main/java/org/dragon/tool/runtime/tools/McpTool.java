package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.mcp.McpHttpClient;
import org.dragon.tool.mcp.McpServerConfig;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.ToolUseContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * MCP 类型工具。
 *
 * <p>通过 {@link McpHttpClient} 调用外部 MCP Server 上的工具。
 * 绑定了特定版本的 executionConfig（serverName + mcpToolName），可安全跨调用复用。
 *
 * <p>实例由 {@link org.dragon.tool.runtime.factory.McpToolFactory} 创建。
 */
@Slf4j
public class McpTool extends AbstractTool<JsonNode, String> {

    private final McpHttpClient mcpHttpClient;
    /** 在 McpToolFactory 中通过 serverName 查找并注入 */
    private final McpServerConfig serverConfig;
    /** MCP Server 上的原始工具名（不含 mcp__ 前缀） */
    private final String mcpToolName;
    private final String toolId;

    /**
     * @param runtime       工具运行时快照（提供 name / description）
     * @param mcpHttpClient 共享的 MCP HTTP 客户端
     * @param serverConfig  已解析好的 MCP Server 配置
     * @param mcpToolName   MCP Server 上的原始工具名
     */
    public McpTool(ToolDefinition runtime,
                   McpHttpClient mcpHttpClient,
                   McpServerConfig serverConfig,
                   String mcpToolName) {
        super(runtime.getName(), runtime.getDescription(), JsonNode.class);
        this.mcpHttpClient = mcpHttpClient;
        this.serverConfig = serverConfig;
        this.mcpToolName = mcpToolName;
        this.toolId = runtime.getToolId();
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("[McpTool] 调用 MCP 工具: tool={}, server={}, mcpTool={}",
                        getName(), serverConfig.getName(), mcpToolName);

                String resultText = mcpHttpClient.callTool(serverConfig, mcpToolName, input);

                log.info("[McpTool] 调用完成: tool={}", getName());
                return ToolResult.ok(resultText);

            } catch (McpHttpClient.McpCallException e) {
                return ToolResult.fail("McpTool: MCP tool '" + mcpToolName
                        + "' on server '" + serverConfig.getName() + "' returned error: " + e.getMessage());
            } catch (McpHttpClient.McpConnectionException e) {
                return ToolResult.fail("McpTool: connection error for server '"
                        + serverConfig.getName() + "': " + e.getMessage());
            } catch (Exception e) {
                return ToolResult.fail("McpTool: unexpected error for tool '"
                        + toolId + "': " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }

    @Override
    public boolean isReadOnly(JsonNode input) {
        // MCP 工具读写属性未知，保守标记为非只读
        return false;
    }
}


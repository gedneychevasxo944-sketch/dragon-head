package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.ToolUseContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * AGENT 类型工具（预留）。
 *
 * <p>调用子 Agent 子任务。LLM 通过此工具将部分任务委托给另一个专门的 Agent 执行（Agent 套 Agent）。
 *
 * <p>实例由 {@link org.dragon.tool.runtime.factory.AgentToolFactory} 创建。
 * AGENT 工具有潜在的子 Agent 会话状态，Factory 声明 {@code isSingleton=false}，
 * ToolRegistry 每次调用前重新构建实例。
 *
 * <p>TODO: 实现子 Agent 调度逻辑，对接 Agent 运行时框架。
 */
@Slf4j
public class AgentTool extends AbstractTool<JsonNode, String> {

    private final String toolId;
    /** 子 Agent 定义配置（来自 executionConfig），待运行时框架接入后解析 */
    private final JsonNode executionConfig;

    /**
     * @param runtime 工具运行时快照（提供 name / description / executionConfig）
     */
    public AgentTool(ToolDefinition runtime) {
        super(runtime.getName(), runtime.getDescription(), JsonNode.class);
        this.toolId = runtime.getToolId();
        this.executionConfig = runtime.getExecutionConfig();
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                           ToolUseContext context,
                                                           Consumer<ToolProgress> progress) {
        // TODO: 实现子 Agent 调度逻辑
        log.warn("[AgentTool] AGENT tool type is not yet implemented: tool={}", getName());
        return CompletableFuture.completedFuture(
                ToolResult.fail("AgentTool: AGENT tool type is not yet implemented. Tool: '" + toolId + "'")
        );
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }

    @Override
    public boolean isReadOnly(JsonNode input) {
        return false;
    }
}


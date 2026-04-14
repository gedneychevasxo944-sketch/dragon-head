package org.dragon.tool.runtime.factory;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolFactory;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.tools.AgentTool;

/**
 * AGENT 类型工具 Factory（预留）。
 *
 * <p>AGENT 工具在未来需要对接子 Agent 运行时框架，可能持有会话级内部状态，
 * 因此 {@link #isSingleton()} 返回 {@code false}——{@link org.dragon.tool.runtime.ToolRegistry}
 * 每次查询时都会重新调用 {@link #create(ToolDefinition)} 构建新实例，避免跨调用状态污染。
 *
 * <p>TODO: 子 Agent 框架接入后，注入 AgentScheduler 等依赖。
 */
public class AgentToolFactory implements ToolFactory {

    @Override
    public ToolType supportedType() {
        return ToolType.AGENT;
    }

    @Override
    public Tool<JsonNode, ?> create(ToolDefinition runtime) {
        return new AgentTool(runtime);
    }

    /**
     * AGENT 工具未来可能有会话级状态，每次调用前重新构建实例。
     */
    @Override
    public boolean isSingleton() {
        return false;
    }
}


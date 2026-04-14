package org.dragon.tool.runtime.factory;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolFactory;
import org.dragon.tool.runtime.ToolDefinition;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ATOMIC 类型工具 Factory。
 *
 * <p>ATOMIC 工具是平台内建的 Java Tool 实例（如 {@code BashTool}、{@code FileReadTool}），
 * 其实例在应用启动时通过 {@link #register(String, Tool)} 注册，Factory 仅负责按类名查找并返回。
 *
 * <p><b>注册时机</b>：应用启动阶段（ToolPlatformService 或 @PostConstruct），
 * 将各 {@code @Component} Tool 注入后逐一调用 {@code register()}。
 */
@Slf4j
public class AtomicToolFactory implements ToolFactory {

    /**
     * 工具类全限定名 → Tool 实例的映射。
     * key 对应 {@code executionConfig.className}，如 {@code "org.dragon.tool.runtime.tools.BashTool"}。
     */
    private final Map<String, Tool<?, ?>> toolInstanceMap = new ConcurrentHashMap<>();

    @Override
    public ToolType supportedType() {
        return ToolType.ATOMIC;
    }

    /**
     * 注册一个内建工具实例（启动时调用）。
     *
     * @param className 工具类全限定名（与 executionConfig.className 对应）
     * @param tool      工具单例实例
     */
    public void register(String className, Tool<?, ?> tool) {
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(tool, "tool must not be null");
        toolInstanceMap.put(className, tool);
        log.info("[AtomicToolFactory] 注册内建工具: className={}, toolName={}", className, tool.getName());
    }

    /**
     * 从已注册的实例 Map 中按 {@code executionConfig.className} 查找并返回 Tool 实例。
     *
     * <p>ATOMIC 工具实例全局唯一（单例），因此忽略 {@code runtime} 中的版本字段，
     * 仅使用 {@code executionConfig.className} 作为查找键。
     *
     * @throws IllegalArgumentException 若 executionConfig 缺少 className 或类名未注册
     */
    @Override
    @SuppressWarnings("unchecked")
    public Tool<JsonNode, ?> create(ToolDefinition runtime) {
        JsonNode config = runtime.getExecutionConfig();
        if (config == null || !config.has("className")) {
            throw new IllegalArgumentException(
                    "AtomicToolFactory: missing 'className' in executionConfig for tool '"
                            + runtime.getToolId() + "'");
        }

        String className = config.path("className").asText();
        Tool<?, ?> tool = toolInstanceMap.get(className);
        if (tool == null) {
            throw new IllegalArgumentException(
                    "AtomicToolFactory: no Tool instance registered for class '"
                            + className + "'. Registered: " + toolInstanceMap.keySet());
        }

        // ATOMIC Tool 已实现 Tool<I,O> where I 是各自强类型（非 JsonNode），
        // ToolRegistry/ToolExecutionService 统一通过 tool.parseInput() + tool.call() 调用，类型安全由运行时保证。
        return (Tool<JsonNode, ?>) tool;
    }

    /**
     * ATOMIC 工具实例是平台内建单例，天然可复用。
     */
    @Override
    public boolean isSingleton() {
        return true;
    }
}


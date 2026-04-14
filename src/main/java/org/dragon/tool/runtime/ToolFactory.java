package org.dragon.tool.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.tool.enums.ToolType;

/**
 * 工具实例工厂接口。
 *
 * <p>每种 {@link ToolType} 对应一个实现，由 {@link ToolRegistry} 在 cache miss 时调用，
 * 将 {@link ToolDefinition}（运行时快照）构建为可直接调用的 {@link Tool} 实例。
 *
 * <p><b>设计要点</b>：
 * <ul>
 *   <li>Factory 本身是无状态单例，由 Spring 注入到 {@link ToolRegistry}；</li>
 *   <li>参数使用 {@link ToolDefinition} 而非 {@link org.dragon.tool.domain.ToolVersionDO}，
 *       这样 Factory 和 {@link Tool} 实现类只依赖纯运行时结构，
 *       与 DB 实体完全解耦（对应 SkillExecutor 只依赖 SkillDefinition 的设计）；</li>
 *   <li>创建出的 {@link Tool} 实例是否复用由 ToolRegistry 的缓存策略决定——
 *       ATOMIC / HTTP / MCP / SKILL 类型的实例可安全复用（无调用级状态），
 *       AGENT / CODE 类型每次调用前应重新创建（实现类通过 {@link #isSingleton()} 声明）；</li>
 *   <li>Factory 不负责执行，仅负责构建——执行逻辑全部封装在返回的 {@link Tool} 实例内。</li>
 * </ul>
 *
 * <h3>与旧 ToolExecutor 的对比：</h3>
 * <pre>
 * 旧：ToolExecutor.execute(version, params, context) → ToolResult
 *     （Factory + Execute 合并在一起，每次调用都重新从 version 读取 config）
 *
 * 新：ToolFactory.create(runtime)          → Tool 实例（含 config 绑定）
 *     Tool.call(params, context, progress) → CompletableFuture&lt;ToolResult&gt;
 *     （Factory 和 Execute 分离，ToolRegistry 负责实例缓存）
 * </pre>
 */
public interface ToolFactory {

    /**
     * 返回此 Factory 负责的工具类型。
     * {@link ToolRegistry} 通过此方法建立 {@code ToolType → ToolFactory} 映射。
     *
     * @return 支持的 {@link ToolType}
     */
    ToolType supportedType();

    /**
     * 从运行时快照构建 {@link Tool} 实例。
     *
     * <p>实现类读取 {@link ToolDefinition#getExecutionConfig()} 中的运行时配置，
     * 结合自身持有的基础设施依赖（如 {@code McpHttpClient}、{@code SkillRegistry} 等），
     * 构建并返回一个可直接调用的 {@link Tool} 实例。
     *
     * @param runtime 工具运行时快照（含 LLM 声明 + executionConfig，不可为 null）
     * @return 对应该版本的 {@link Tool} 实例
     * @throws IllegalArgumentException 若 executionConfig 缺少必要字段
     */
    Tool<JsonNode, ?> create(ToolDefinition runtime);

    /**
     * 创建出的实例是否可以跨调用复用（单例缓存）。
     *
     * <p>返回 {@code true} 时，{@link ToolRegistry} 会将实例缓存（Caffeine），
     * 下次 {@code findByName} 命中相同 key 时直接复用，不重新调用 {@link #create}。<br>
     * 返回 {@code false} 时，每次 {@code findByName} 都会调用 {@link #create} 新建实例，
     * 适用于有调用级内部状态的工具（如 AGENT 类型）。
     *
     * <p>默认返回 {@code true}（绝大多数工具无状态，可安全复用）。
     *
     * @return {@code true} 表示实例可复用
     */
    default boolean isSingleton() {
        return true;
    }
}


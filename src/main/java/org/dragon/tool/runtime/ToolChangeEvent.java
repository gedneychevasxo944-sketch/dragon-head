package org.dragon.tool.runtime;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Tool 变更事件（缓存失效机制）。
 *
 * <p>以下场景触发发布：
 * <ul>
 *   <li>Tool 状态变更（publish / disable / delete）— 由 ToolLifeCycleService 发布</li>
 *   <li>Tool 绑定关系增删 — 由 ToolBindingService 发布</li>
 *   <li>内置工具重新加载（如 MCP Server 重连）— 发布全量失效事件</li>
 * </ul>
 *
 * <p>ToolRegistry 监听此事件：
 * <ul>
 *   <li>精确失效：按 affectedCharacterIds / affectedWorkspaceIds 失效对应缓存条目</li>
 *   <li>全量失效：affectedCharacterIds 与 affectedWorkspaceIds 均为空时（{@link #isGlobalEvict()}），
 *       清空所有缓存。内置 Tool 变更时直接用 {@link #ofAll} 发布即可。</li>
 * </ul>
 *
 * <p>对齐 {@code SkillChangeEvent} 的设计风格。
 */
@Getter
public class ToolChangeEvent extends ApplicationEvent {

    /**
     * 受影响的 characterId 集合。
     * Tool 绑定变更时，直接填受影响的 characterId。
     * Tool 状态变更时，需找到绑定了该 toolId 的所有 characterId。
     */
    private final Set<String> affectedCharacterIds;

    /**
     * 受影响的 workspaceId 集合。
     * 逻辑同 affectedCharacterIds。
     */
    private final Set<String> affectedWorkspaceIds;

    /**
     * 变更原因（便于日志追踪）。
     */
    private final String reason;

    public ToolChangeEvent(Object source,
                           Collection<String> affectedCharacterIds,
                           Collection<String> affectedWorkspaceIds,
                           String reason) {
        super(source);
        this.affectedCharacterIds = affectedCharacterIds != null
                ? Set.copyOf(affectedCharacterIds) : Collections.emptySet();
        this.affectedWorkspaceIds = affectedWorkspaceIds != null
                ? Set.copyOf(affectedWorkspaceIds) : Collections.emptySet();
        this.reason = reason;
    }

    /**
     * 创建一个"全量失效"事件（affectedCharacterIds/workspaceIds 均为空）。
     *
     * <p>适用场景：
     * <ul>
     *   <li>builtin=true 的 Tool 状态变更（内置 Tool 对所有 Character 全量可见）</li>
     *   <li>MCP Server 重连后工具列表变化</li>
     *   <li>其他无法精确定位受影响范围的系统级变更</li>
     * </ul>
     */
    public static ToolChangeEvent ofAll(Object source, String reason) {
        return new ToolChangeEvent(source, Collections.emptySet(), Collections.emptySet(), reason);
    }

    /**
     * 创建 workspace 维度的精确失效事件。
     *
     * @param source      事件来源（通常是 this）
     * @param workspaceId 受影响的 workspaceId
     * @param reason      变更原因
     */
    public static ToolChangeEvent ofWorkspace(Object source, String workspaceId, String reason) {
        return new ToolChangeEvent(source, Collections.emptySet(),
                Collections.singletonList(workspaceId), reason);
    }

    /**
     * 创建 character 维度的精确失效事件。
     *
     * @param source      事件来源（通常是 this）
     * @param characterId 受影响的 characterId
     * @param reason      变更原因
     */
    public static ToolChangeEvent ofCharacter(Object source, String characterId, String reason) {
        return new ToolChangeEvent(source, Collections.singletonList(characterId),
                Collections.emptySet(), reason);
    }

    /**
     * 是否是全量失效事件（affectedCharacterIds 与 affectedWorkspaceIds 均为空）。
     */
    public boolean isGlobalEvict() {
        return affectedCharacterIds.isEmpty() && affectedWorkspaceIds.isEmpty();
    }
}


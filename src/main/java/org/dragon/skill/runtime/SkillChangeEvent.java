package org.dragon.skill.runtime;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Skill 变更事件（设计点 3 - 缓存失效机制）。
 *
 * <p>以下场景触发发布：
 * <ul>
 *   <li>Skill 状态变更（publish / disable / delete）— 由 SkillLifecycleService 发布</li>
 *   <li>Skill 关联关系增删 — 由 AssetAssociationService 完成后发布</li>
 * </ul>
 *
 * <p>SkillRegistry 监听此事件：
 * <ul>
 *   <li>精确失效：按 affectedCharacterIds / affectedWorkspaceIds 失效对应缓存条目</li>
 *   <li>全量失效：affectedCharacterIds 与 affectedWorkspaceIds 均为空时（{@link #isGlobalEvict()}），
 *       清空所有缓存。builtin Skill 变更时直接用 {@link #ofAll} 发布即可，无需额外标志。</li>
 * </ul>
 */
@Getter
public class SkillChangeEvent extends ApplicationEvent {

    /**
     * 受影响的 characterId 集合。
     * Skill 状态变更时，需要找到绑定了该 skillId 的所有 characterId。
     * Skill 绑定变更时，直接填受影响的 characterId。
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

    public SkillChangeEvent(Object source,
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
     *   <li>builtin=true 的 Skill 状态变更（内置 Skill 对所有 Character 全量可见）</li>
     *   <li>其他无法精确定位受影响范围的系统级变更</li>
     * </ul>
     */
    public static SkillChangeEvent ofAll(Object source, String reason) {
        return new SkillChangeEvent(source, Collections.emptySet(), Collections.emptySet(), reason);
    }

    /**
     * 是否是全量失效事件（affectedCharacterIds 与 affectedWorkspaceIds 均为空）。
     */
    public boolean isGlobalEvict() {
        return affectedCharacterIds.isEmpty() && affectedWorkspaceIds.isEmpty();
    }

}


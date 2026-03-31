package org.dragon.skill.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Skill 绑定变更事件（Workspace 维度）。
 *
 * 由 SkillChangeListener 在处理 SkillChangedEvent 后，
 * 查询 skill_bind 关联表确定影响范围，再为每个受影响的 workspace 发布此事件。
 *
 * 消费者：
 * - SandboxSkillSyncListener：负责刷新 sandbox 中的 skill 文件
 * - AgentSkillRefreshListener：负责通知 agent 刷新 System Prompt
 *
 * @since 1.0
 */
@Getter
public class SkillBindingChangedEvent extends ApplicationEvent {

    /**
     * Workspace 应执行的动作类型。
     */
    public enum ActionType {
        /** 需要热更新 skill 文件和运行时（useLatest=true 的版本更新） */
        RELOAD,
        /** 仅通知 agent 刷新 System Prompt（元数据变更） */
        REFRESH_PROMPT,
        /** 从 workspace 运行时中移除该 skill（全局禁用/删除） */
        REMOVE
    }

    /** 受影响的 workspace ID */
    private final Long workspaceId;

    /** 变更的 skill ID */
    private final Long skillId;

    /** skill 名称 */
    private final String skillName;

    /** workspace 应执行的动作 */
    private final ActionType actionType;

    /**
     * 需要加载的目标版本（actionType=RELOAD 时有效）。
     * 来自 skill_bind.pinnedVersion（已被 SkillBindingService 更新为最新）。
     */
    private final Integer targetVersion;

    private final LocalDateTime occurredAt;

    public SkillBindingChangedEvent(Object source, Long workspaceId, Long skillId,
                                       String skillName, ActionType actionType,
                                       Integer targetVersion) {
        super(source);
        this.workspaceId = workspaceId;
        this.skillId = skillId;
        this.skillName = skillName;
        this.actionType = actionType;
        this.targetVersion = targetVersion;
        this.occurredAt = LocalDateTime.now();
    }
}

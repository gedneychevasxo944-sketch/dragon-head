package org.dragon.skill.event;

import lombok.Getter;
import org.dragon.skill.entity.SkillEntity;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Skill 变更事件。
 * 当 Skill 发生变更（创建、更新、删除、禁用、激活）时发布此事件。
 *
 * @since 1.0
 */
@Getter
public class SkillChangedEvent extends ApplicationEvent {

    /**
     * 变更类型枚举。
     */
    public enum ChangeType {
        /** 新增 Skill */
        CREATED,
        /** Skill 版本更新 */
        VERSION_UPDATED,
        /** Skill 管理元数据更新 */
        META_UPDATED,
        /** Skill 被删除 */
        DELETED,
        /** Skill 被禁用 */
        DISABLED,
        /** Skill 被重新激活 */
        ACTIVATED
    }

    /** 发生变更的 Skill ID */
    private final Long skillId;

    /** Skill 名称 */
    private final String skillName;

    /** 变更类型 */
    private final ChangeType changeType;

    /** 变更后的版本号 */
    private final Integer version;

    /** 事件发生时间 */
    private final LocalDateTime occurredAt;

    public SkillChangedEvent(Object source, Long skillId, String skillName,
                              ChangeType changeType, Integer version) {
        super(source);
        this.skillId = skillId;
        this.skillName = skillName;
        this.changeType = changeType;
        this.version = version;
        this.occurredAt = LocalDateTime.now();
    }

    /**
     * 从 SkillEntity 创建事件。
     */
    public static SkillChangedEvent fromEntity(Object source, SkillEntity entity, ChangeType changeType) {
        return new SkillChangedEvent(
                source,
                entity.getId(),
                entity.getName(),
                changeType,
                entity.getVersion()
        );
    }
}

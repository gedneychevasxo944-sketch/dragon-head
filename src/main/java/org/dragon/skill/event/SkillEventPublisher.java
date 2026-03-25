package org.dragon.skill.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.entity.SkillEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Skill 事件发布器。
 * 封装事件发布逻辑。
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCreated(SkillEntity entity) {
        publish(entity, SkillChangedEvent.ChangeType.CREATED);
    }

    public void publishVersionUpdated(SkillEntity entity) {
        publish(entity, SkillChangedEvent.ChangeType.VERSION_UPDATED);
    }

    public void publishMetaUpdated(SkillEntity entity) {
        publish(entity, SkillChangedEvent.ChangeType.META_UPDATED);
    }

    public void publishDeleted(SkillEntity entity) {
        publish(entity, SkillChangedEvent.ChangeType.DELETED);
    }

    public void publishDisabled(SkillEntity entity) {
        publish(entity, SkillChangedEvent.ChangeType.DISABLED);
    }

    public void publishActivated(SkillEntity entity) {
        publish(entity, SkillChangedEvent.ChangeType.ACTIVATED);
    }

    private void publish(SkillEntity entity, SkillChangedEvent.ChangeType changeType) {
        SkillChangedEvent event = SkillChangedEvent.fromEntity(this, entity, changeType);
        log.info("发布 Skill 变更事件: skillId={}, name={}, changeType={}, version={}",
                entity.getId(), entity.getName(), changeType, entity.getVersion());
        eventPublisher.publishEvent(event);
    }
}

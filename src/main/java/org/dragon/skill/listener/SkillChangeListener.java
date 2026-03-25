package org.dragon.skill.listener;

import org.dragon.sandbox.manager.SandboxManager;
import org.dragon.skill.event.SkillChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Skill 变更监听器。
 *
 * 监听 Skill 变更事件，触发相关 sandbox 的文件刷新。
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillChangeListener {

    private final SandboxManager sandboxManager;

    /**
     * 处理 Skill 版本更新事件。
     */
    @Async
    @EventListener
    public void onSkillVersionUpdated(SkillChangedEvent event) {
        if (event.getChangeType() != SkillChangedEvent.ChangeType.VERSION_UPDATED) {
            return;
        }

        log.info("Skill 版本已更新，刷新 sandbox 文件: name={}, workspaceId={}",
                event.getSkillName(), event.getWorkspaceId());

        sandboxManager.refreshSkillInSandboxes(
                event.getSkillName(), event.getWorkspaceId());
    }

    /**
     * 处理 Skill 删除事件。
     */
    @Async
    @EventListener
    public void onSkillDeleted(SkillChangedEvent event) {
        if (event.getChangeType() != SkillChangedEvent.ChangeType.DELETED) {
            return;
        }

        log.info("Skill 已删除，刷新 sandbox 文件: name={}, workspaceId={}",
                event.getSkillName(), event.getWorkspaceId());

        sandboxManager.refreshSkillInSandboxes(
                event.getSkillName(), event.getWorkspaceId());
    }

    /**
     * 处理 Skill 创建事件（新 Skill 需要同步到 sandbox）。
     */
    @Async
    @EventListener
    public void onSkillCreated(SkillChangedEvent event) {
        if (event.getChangeType() != SkillChangedEvent.ChangeType.CREATED) {
            return;
        }

        log.info("Skill 已创建，同步到 sandbox: name={}, workspaceId={}",
                event.getSkillName(), event.getWorkspaceId());

        sandboxManager.refreshSkillInSandboxes(
                event.getSkillName(), event.getWorkspaceId());
    }
}
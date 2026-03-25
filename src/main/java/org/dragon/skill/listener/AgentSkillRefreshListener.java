package org.dragon.skill.listener;

import org.dragon.skill.event.WorkspaceSkillChangedEvent;
import org.dragon.skill.registry.SkillRegistry;
import org.dragon.skill.service.SkillLoaderService;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.WorkspaceSkillStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Agent 运行时 Skill 刷新监听器（第二层）。
 *
 * 职责：
 * 监听 WorkspaceSkillChangedEvent，
 * 根据 actionType 更新 SkillRegistry 中对应 workspace 的运行时状态，
 * 使 Agent 下次构建 System Prompt 时能获取到最新内容。
 *
 * 注意：只处理运行时注册表，不涉及文件操作。
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentSkillRefreshListener {

    private final SkillRegistry skillRegistry;
    private final SkillLoaderService loaderService;
    private final SkillStore skillStore;
    private final WorkspaceSkillStore workspaceSkillStore;

    @Async
    @EventListener
    public void onWorkspaceSkillChanged(WorkspaceSkillChangedEvent event) {
        log.info("收到 WorkspaceSkill 变更事件: workspaceId={}, skillName={}, action={}",
                event.getWorkspaceId(), event.getSkillName(), event.getActionType());

        switch (event.getActionType()) {
            case RELOAD          -> handleReload(event);
            case REFRESH_PROMPT  -> handleRefreshPrompt(event);
            case REMOVE          -> handleRemove(event);
        }
    }

    /**
     * 重新加载：从数据库重新构建 SkillEntry，更新 workspace 注册表分区。
     * 文件已由 SandboxSkillSyncListener 更新，此处只更新运行时对象。
     */
    private void handleReload(WorkspaceSkillChangedEvent event) {
        log.info("刷新 workspace 运行时 Skill: workspaceId={}, skillName={}, version={}",
                event.getWorkspaceId(), event.getSkillName(), event.getTargetVersion());

        skillStore.findById(event.getSkillId()).ifPresentOrElse(
                skill -> loaderService.loadSkillForWorkspace(
                        skill,
                        event.getWorkspaceId(),
                        event.getTargetVersion()
                ),
                () -> log.warn("Skill 不存在，无法重新加载: skillId={}", event.getSkillId())
        );
    }

    /**
     * 刷新 System Prompt：运行时 SkillEntry 内容不变，
     * 但 skill_description 或 skill_content 可能已更新，
     * 需要重新从数据库加载最新内容到注册表。
     */
    private void handleRefreshPrompt(WorkspaceSkillChangedEvent event) {
        log.info("刷新 workspace System Prompt: workspaceId={}, skillName={}",
                event.getWorkspaceId(), event.getSkillName());

        // 查询该 workspace 对此 skill 的锁定版本
        workspaceSkillStore
                .findByWorkspaceIdAndSkillId(event.getWorkspaceId(), event.getSkillId())
                .ifPresent(binding ->
                        skillStore.findById(event.getSkillId()).ifPresent(skill ->
                                loaderService.loadSkillForWorkspace(
                                        skill,
                                        event.getWorkspaceId(),
                                        binding.getPinnedVersion()
                                )
                        )
                );
    }

    /**
     * 移除：从 workspace 注册表分区中注销该 skill。
     */
    private void handleRemove(WorkspaceSkillChangedEvent event) {
        log.info("从 workspace 运行时移除 Skill: workspaceId={}, skillName={}",
                event.getWorkspaceId(), event.getSkillName());

        skillRegistry.unregisterForWorkspace(
                event.getSkillName(),
                event.getWorkspaceId()
        );
    }
}

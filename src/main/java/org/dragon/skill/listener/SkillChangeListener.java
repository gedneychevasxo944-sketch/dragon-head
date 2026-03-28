package org.dragon.skill.listener;

import org.dragon.skill.entity.WorkspaceSkillEntity;
import org.dragon.skill.event.SkillChangedEvent;
import org.dragon.skill.event.SkillEventPublisher;
import org.dragon.skill.event.WorkspaceSkillChangedEvent;
import org.dragon.skill.service.SkillLoaderService;
import org.dragon.skill.store.WorkspaceSkillStore;
import org.dragon.store.StoreFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill 全局变更监听器（第一层）。
 *
 * 职责：
 * 1. 接收 SkillChangedEvent（skill 维度的变更）
 * 2. 查询 workspace_skill 关联表，确定受影响的 workspace 范围
 * 3. 根据变更类型和 useLatest 策略，为每个受影响的 workspace
 *    发布 WorkspaceSkillChangedEvent（workspace 维度的变更）
 *
 * 注意：此监听器只做"事件路由"，不直接操作 sandbox 或 registry，
 * 具体操作由下游监听器完成，保持职责单一。
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillChangeListener {

    private final SkillEventPublisher eventPublisher;
    private final SkillLoaderService loaderService;
    private final WorkspaceSkillStore workspaceSkillStore;

    @Async
    @EventListener
    public void onSkillChanged(SkillChangedEvent event) {
        log.info("收到 Skill 全局变更事件: skillId={}, name={}, changeType={}",
                event.getSkillId(), event.getSkillName(), event.getChangeType());

        switch (event.getChangeType()) {
            case VERSION_UPDATED -> handleVersionUpdated(event);
            case META_UPDATED    -> handleMetaUpdated(event);
            case DELETED         -> handleDeleted(event);
            case DISABLED        -> handleDisabled(event);
            case ACTIVATED       -> handleActivated(event);
            case CREATED         -> handleCreated(event);
            default -> log.warn("未处理的变更类型: {}", event.getChangeType());
        }
    }

    /**
     * Skill 新版本发布。
     *
     * 路由策略：
     * - useLatest=true 的 workspace → 发布 RELOAD 事件（需热更新文件和运行时）
     * - useLatest=false 的 workspace → 不通知（锁定版本，不受影响）
     */
    private void handleVersionUpdated(SkillChangedEvent event) {
        List<WorkspaceSkillEntity> followers =
                workspaceSkillStore.findAllUseLatestBySkillId(event.getSkillId());

        if (followers.isEmpty()) {
            log.info("Skill [{}] 无 useLatest=true 的 workspace 订阅，跳过通知",
                    event.getSkillName());
            return;
        }

        log.info("Skill [{}] 版本更新，通知 {} 个 workspace 热更新",
                event.getSkillName(), followers.size());

        followers.forEach(ws ->
                eventPublisher.publishWorkspaceSkillChanged(
                        ws.getWorkspaceId(),
                        event.getSkillId(),
                        event.getSkillName(),
                        WorkspaceSkillChangedEvent.ActionType.RELOAD,
                        event.getVersion()   // 目标版本 = 最新版本
                )
        );
    }

    /**
     * Skill 元数据更新（非版本升级，如 description、tags 变更）。
     *
     * 路由策略：
     * - 所有圈选了此 skill 的 workspace → 发布 REFRESH_PROMPT 事件
     *   （description 变更会影响 LLM 的调用判断，需刷新 System Prompt）
     */
    private void handleMetaUpdated(SkillChangedEvent event) {
        List<Long> affectedWorkspaces =
                workspaceSkillStore.findWorkspaceIdsBySkillId(event.getSkillId());

        log.info("Skill [{}] 元数据更新，通知 {} 个 workspace 刷新 Prompt",
                event.getSkillName(), affectedWorkspaces.size());

        affectedWorkspaces.forEach(wsId ->
                eventPublisher.publishWorkspaceSkillChanged(
                        wsId,
                        event.getSkillId(),
                        event.getSkillName(),
                        WorkspaceSkillChangedEvent.ActionType.REFRESH_PROMPT,
                        null
                )
        );
    }

    /**
     * Skill 被全局删除。
     *
     * 路由策略：
     * - 所有圈选了此 skill 的 workspace → 发布 REMOVE 事件
     */
    private void handleDeleted(SkillChangedEvent event) {
        List<Long> affectedWorkspaces =
                workspaceSkillStore.findWorkspaceIdsBySkillId(event.getSkillId());

        log.info("Skill [{}] 已删除，通知 {} 个 workspace 移除",
                event.getSkillName(), affectedWorkspaces.size());

        affectedWorkspaces.forEach(wsId ->
                eventPublisher.publishWorkspaceSkillChanged(
                        wsId,
                        event.getSkillId(),
                        event.getSkillName(),
                        WorkspaceSkillChangedEvent.ActionType.REMOVE,
                        null
                )
        );
    }

    /**
     * Skill 被全局禁用。
     * 策略与删除相同：所有相关 workspace 移除该 skill。
     */
    private void handleDisabled(SkillChangedEvent event) {
        List<Long> affectedWorkspaces =
                workspaceSkillStore.findWorkspaceIdsBySkillId(event.getSkillId());

        log.info("Skill [{}] 已全局禁用，通知 {} 个 workspace 移除",
                event.getSkillName(), affectedWorkspaces.size());

        affectedWorkspaces.forEach(wsId ->
                eventPublisher.publishWorkspaceSkillChanged(
                        wsId,
                        event.getSkillId(),
                        event.getSkillName(),
                        WorkspaceSkillChangedEvent.ActionType.REMOVE,
                        null
                )
        );
    }

    /**
     * Skill 从禁用状态重新激活。
     *
     * 路由策略：
     * - useLatest=true 的 workspace → RELOAD（重新加载最新版本）
     * - useLatest=false 的 workspace → RELOAD（重新加载锁定版本）
     * - 两者都需要重新加载，因为之前 REMOVE 时已从运行时移除
     */
    private void handleActivated(SkillChangedEvent event) {
        List<WorkspaceSkillEntity> allBindings =
                workspaceSkillStore.findAllEnabledBySkillId(event.getSkillId());

        log.info("Skill [{}] 重新激活，通知 {} 个 workspace 重新加载",
                event.getSkillName(), allBindings.size());

        allBindings.forEach(ws ->
                eventPublisher.publishWorkspaceSkillChanged(
                        ws.getWorkspaceId(),
                        event.getSkillId(),
                        event.getSkillName(),
                        WorkspaceSkillChangedEvent.ActionType.RELOAD,
                        ws.getPinnedVersion()  // 各 workspace 使用自己锁定的版本
                )
        );
    }

    /**
     * Skill 新建。
     * 新建时不存在任何 workspace 关联，无需通知。
     * workspace 主动圈选时才会触发加载。
     */
    private void handleCreated(SkillChangedEvent event) {
        log.info("Skill [{}] 已创建，等待 workspace 主动圈选", event.getSkillName());
    }
}

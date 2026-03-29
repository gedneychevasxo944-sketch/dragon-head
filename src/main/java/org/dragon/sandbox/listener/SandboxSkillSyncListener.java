package org.dragon.sandbox.listener;

import org.dragon.sandbox.domain.Sandbox;
import org.dragon.sandbox.manager.SandboxManager;
import org.dragon.skill.event.SkillBindingChangedEvent;
import org.dragon.skill.service.SkillSandboxSupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sandbox 文件同步监听器（第二层）。
 *
 * 职责：
 * 监听 SkillBindingChangedEvent，
 * 根据 actionType 对 sandbox 中的 skill 文件执行对应操作。
 *
 * 注意：只处理文件层面的操作，不涉及 registry 和 agent。
 *
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SandboxSkillSyncListener {

    private final SandboxManager sandboxManager;
    private final SkillSandboxSupportService skillSandboxSupportService;

    @Async
    @EventListener
    public void onSkillBindingChanged(SkillBindingChangedEvent event) {
        // 若该 workspace 的 sandbox 尚未创建，无需处理（创建时会全量同步）
        Sandbox sandbox = sandboxManager.get(event.getWorkspaceId()).orElse(null);
        if (sandbox == null) {
            log.debug("workspace [{}] 的 sandbox 尚未创建，跳过文件同步",
                    event.getWorkspaceId());
            return;
        }

        switch (event.getActionType()) {
            case RELOAD -> handleReload(event, sandbox);
            case REMOVE -> handleRemove(event, sandbox);
            case REFRESH_PROMPT -> {
                // 元数据变更不涉及文件操作，sandbox 无需处理
                log.debug("REFRESH_PROMPT 事件，sandbox 无需处理文件: skillName={}",
                        event.getSkillName());
            }
        }
    }

    /**
     * 重新加载：删除旧版本文件，下载新版本文件。
     */
    private void handleReload(SkillBindingChangedEvent event, Sandbox sandbox) {
        log.info("Sandbox 文件热更新: workspaceId={}, skillName={}, targetVersion={}",
                event.getWorkspaceId(), event.getSkillName(), event.getTargetVersion());
        try {
            skillSandboxSupportService.syncSingleSkillToSandbox(
                    event.getSkillName(),
                    sandbox.getSkillsDir(),
                    event.getTargetVersion()   // 传入目标版本，下载对应版本的文件
            );
        } catch (Exception e) {
            log.error("Sandbox 文件热更新失败: workspaceId={}, skillName={}",
                    event.getWorkspaceId(), event.getSkillName(), e);
        }
    }

    /**
     * 移除：删除 sandbox 中该 skill 的文件目录。
     */
    private void handleRemove(SkillBindingChangedEvent event, Sandbox sandbox) {
        log.info("Sandbox 移除 Skill 文件: workspaceId={}, skillName={}",
                event.getWorkspaceId(), event.getSkillName());
        try {
            skillSandboxSupportService.removeSkillFromSandbox(
                    event.getSkillName(),
                    sandbox.getSkillsDir()
            );
        } catch (Exception e) {
            log.error("Sandbox 移除 Skill 文件失败: workspaceId={}, skillName={}",
                    event.getWorkspaceId(), event.getSkillName(), e);
        }
    }
}

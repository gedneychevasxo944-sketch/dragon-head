package org.dragon.tool.service;

import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.actionlog.ActionType;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolVersionStatus;
import org.dragon.tool.runtime.ToolChangeEvent;
import org.dragon.tool.store.ToolStore;
import org.dragon.tool.store.ToolVersionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 工具生命周期服务。
 *
 * <p>负责工具及版本状态的流转管理（对齐 SkillLifeCycleService 设计风格）：
 *
 * <pre>
 * 工具状态流转：
 *   [注册] ──→ DRAFT ──→ ACTIVE ⇄ DISABLED
 *
 * 版本状态流转：
 *   draft → published → deprecated
 * </pre>
 *
 * <p><b>发布逻辑</b>：
 * <ul>
 *   <li>发布时将目标 draft 版本设为 {@link ToolVersionStatus#PUBLISHED}</li>
 *   <li>将之前的已发布版本设为 {@link ToolVersionStatus#DEPRECATED}</li>
 *   <li>将 {@link ToolDO#getPublishedVersionId()} 指向新版本的 id</li>
 *   <li>将 ToolDO 状态设为 {@link ToolStatus#ACTIVE}（若之前为 DRAFT）</li>
 * </ul>
 */
@Service
public class ToolLifeCycleService {

    private static final Logger log = LoggerFactory.getLogger(ToolLifeCycleService.class);

    private final ToolStore toolStore;
    private final ToolVersionStore toolVersionStore;
    private final ToolActionLogService actionLogService;
    private final ApplicationEventPublisher eventPublisher;

    public ToolLifeCycleService(ToolStore toolStore,
                                ToolVersionStore toolVersionStore,
                                ToolActionLogService actionLogService,
                                ApplicationEventPublisher eventPublisher) {
        this.toolStore = Objects.requireNonNull(toolStore, "toolStore must not be null");
        this.toolVersionStore = Objects.requireNonNull(toolVersionStore, "toolVersionStore must not be null");
        this.actionLogService = Objects.requireNonNull(actionLogService, "actionLogService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    // ── 版本发布 ─────────────────────────────────────────────────────────

    /**
     * 发布指定版本（DRAFT → PUBLISHED）。
     *
     * <p>发布后：
     * <ol>
     *   <li>将目标版本状态设为 {@link ToolVersionStatus#PUBLISHED}，记录 publishedAt</li>
     *   <li>将旧的已发布版本设为 {@link ToolVersionStatus#DEPRECATED}</li>
     *   <li>更新 {@link ToolDO#getPublishedVersionId()} 指向新版本</li>
     *   <li>若 ToolDO 状态为 DRAFT，同步设为 {@link ToolStatus#ACTIVE}</li>
     * </ol>
     *
     * @param toolId       工具 ID
     * @param versionId    目标版本的物理 ID（{@link ToolVersionDO#getId()}，必须为 DRAFT 状态）
     * @param operatorId   操作人 ID
     * @param operatorName 操作人名称
     * @param releaseNote  发版备注（可为 null）
     * @throws IllegalArgumentException 如果工具或版本不存在
     * @throws IllegalStateException    如果版本不处于 DRAFT 状态
     */
    public void publish(String toolId, Long versionId,
                        Long operatorId, String operatorName, String releaseNote) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        Objects.requireNonNull(versionId, "versionId must not be null");

        ToolDO tool = requireTool(toolId);

        ToolVersionDO newVersion = toolVersionStore.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version not found: " + versionId));

        if (!newVersion.getToolId().equals(toolId)) {
            throw new IllegalArgumentException(
                    "Version '" + versionId + "' does not belong to tool '" + toolId + "'");
        }
        if (newVersion.getStatus() != ToolVersionStatus.DRAFT) {
            throw new IllegalStateException(
                    "Version '" + versionId + "' is not in DRAFT status. Current: "
                            + newVersion.getStatus());
        }

        // 将旧的已发布版本设为 DEPRECATED
        toolVersionStore.findByToolIdAndStatus(toolId, ToolVersionStatus.PUBLISHED)
                .forEach(oldVersion -> {
                    oldVersion.setStatus(ToolVersionStatus.DEPRECATED);
                    toolVersionStore.update(oldVersion);
                });

        // 发布新版本
        newVersion.setStatus(ToolVersionStatus.PUBLISHED);
        newVersion.setPublishedAt(LocalDateTime.now());
        toolVersionStore.update(newVersion);

        // 更新 ToolDO.publishedVersionId 指针
        tool.setPublishedVersionId(versionId);
        if (tool.getStatus() == ToolStatus.DRAFT) {
            tool.setStatus(ToolStatus.ACTIVE);
        }
        syncToolDO(tool);

        actionLogService.log(toolId, tool.getName(), tool.getDisplayName(),
                ActionType.TOOL_PUBLISH, operatorName, newVersion.getVersion(),
                releaseNote != null ? Map.of("releaseNote", releaseNote) : null);

        // 版本切换后缓存内容已过期，全量失效
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "publish: toolId=" + toolId + ", versionId=" + versionId));

        log.info("[ToolLifeCycleService] 版本发布: toolId={}, versionId={}", toolId, versionId);
    }

    // ── 工具状态管理 ─────────────────────────────────────────────────────

    /**
     * 禁用工具（ACTIVE → DISABLED）。
     *
     * <p>禁用后 ToolRegistry 缓存立即失效，工具在后续查询中不再可见，
     * 运行时调用该工具将返回错误。
     *
     * @param toolId       工具 ID
     * @param operatorId   操作人 ID
     * @param operatorName 操作人名称
     * @throws IllegalStateException 如果工具已是 DISABLED 状态
     */
    public void disable(String toolId, Long operatorId, String operatorName) {
        ToolDO tool = requireTool(toolId);
        if (tool.getStatus() == ToolStatus.DISABLED) {
            throw new IllegalStateException(
                    "Tool '" + toolId + "' is already DISABLED.");
        }

        tool.setStatus(ToolStatus.DISABLED);
        syncToolDO(tool);

        actionLogService.log(toolId, tool.getName(), tool.getDisplayName(),
                ActionType.TOOL_DISABLE, operatorName, (Integer) null);

        // 工具禁用后对所有 character/workspace 不再可见，全量失效
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "disable: toolId=" + toolId));

        log.info("[ToolLifeCycleService] 工具禁用: toolId={}", toolId);
    }

    /**
     * 重新启用工具（DISABLED → ACTIVE）。
     *
     * <p>仅当工具有已发布版本（publishedVersionId != null）时才可启用。
     *
     * @param toolId       工具 ID
     * @param operatorId   操作人 ID
     * @param operatorName 操作人名称
     * @throws IllegalStateException 如果工具无已发布版本或已是 ACTIVE
     */
    public void enable(String toolId, Long operatorId, String operatorName) {
        ToolDO tool = requireTool(toolId);
        if (tool.getStatus() == ToolStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Tool '" + toolId + "' is already ACTIVE.");
        }
        if (tool.getPublishedVersionId() == null) {
            throw new IllegalStateException(
                    "Tool '" + toolId + "' has no published version. Publish a version first.");
        }

        tool.setStatus(ToolStatus.ACTIVE);
        syncToolDO(tool);

        actionLogService.log(toolId, tool.getName(), tool.getDisplayName(),
                ActionType.TOOL_ENABLE, operatorName, (Integer) null);

        // 工具重新启用后需刷新缓存，全量失效
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "enable: toolId=" + toolId));

        log.info("[ToolLifeCycleService] 工具启用: toolId={}", toolId);
    }

    // ── 状态查询 ─────────────────────────────────────────────────────────

    /**
     * 获取工具当前状态。
     *
     * @param toolId 工具 ID
     * @return 工具状态
     * @throws IllegalArgumentException 如果工具不存在
     */
    public ToolStatus getStatus(String toolId) {
        return requireTool(toolId).getStatus();
    }

    // ── 内部工具方法 ─────────────────────────────────────────────────────

    private ToolDO requireTool(String toolId) {
        return toolStore.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tool not found: " + toolId));
    }

    /**
     * 持久化 ToolDO 状态变更。
     */
    private void syncToolDO(ToolDO tool) {
        toolStore.update(tool);
    }
}

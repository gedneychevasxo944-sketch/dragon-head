package org.dragon.tool.service;

import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.actionlog.ActionType;
import org.dragon.tool.runtime.ToolChangeEvent;
import org.dragon.tool.store.ToolStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工具绑定服务。
 *
 * <p>负责工具与 workspace / character 之间关联关系的 CRUD 管理，底层使用
 * {@link AssetAssociationService}（TOOL_WORKSPACE / TOOL_CHARACTER 关联类型）。
 * 绑定关系决定了哪些非内置工具在哪个 workspace 或 character 下可见可用。
 *
 * <p><b>可见性规则</b>：
 * <pre>
 * 当前 Character 可用工具 =
 *     内置工具（builtin=true, status=ACTIVE）
 *   ∪ workspace 关联工具（TOOL_WORKSPACE）
 *   ∪ character 关联工具（TOOL_CHARACTER）
 * </pre>
 *
 * <p><b>缓存失效</b>：绑定/解绑操作完成后，发布 {@link ToolChangeEvent} 通知
 * {@link org.dragon.tool.runtime.ToolRegistry} 精确失效对应缓存条目。
 */
@Service
public class ToolBindingService {

    private static final Logger log = LoggerFactory.getLogger(ToolBindingService.class);

    private final AssetAssociationService assetAssociationService;
    private final ToolStore toolStore;
    private final ToolActionLogService actionLogService;
    private final ApplicationEventPublisher eventPublisher;

    public ToolBindingService(AssetAssociationService assetAssociationService,
                              ToolStore toolStore,
                              ToolActionLogService actionLogService,
                              ApplicationEventPublisher eventPublisher) {
        this.assetAssociationService = Objects.requireNonNull(assetAssociationService, "assetAssociationService must not be null");
        this.toolStore = Objects.requireNonNull(toolStore, "toolStore must not be null");
        this.actionLogService = Objects.requireNonNull(actionLogService, "actionLogService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    // ── 绑定操作 ─────────────────────────────────────────────────────────

    /**
     * 将工具关联到 workspace（该 workspace 下的所有 Character 均可使用）。
     *
     * @param toolId        工具 ID（必须已存在）
     * @param workspaceId   目标 workspace ID
     * @param workspaceName workspace 名称（用于日志展示）
     * @param operatorId    操作人 ID
     * @param operatorName  操作人名称
     * @throws IllegalArgumentException 如果工具不存在
     */
    public void bindToWorkspace(String toolId, String workspaceId,
                                String workspaceName,
                                Long operatorId, String operatorName) {
        validateToolExists(toolId);
        assetAssociationService.createAssociation(
                AssociationType.TOOL_WORKSPACE,
                ResourceType.TOOL, toolId,
                ResourceType.WORKSPACE, workspaceId);

        String toolName = toolStore.findById(toolId).map(t -> t.getName()).orElse(toolId);
        actionLogService.log(toolId, toolName, ActionType.TOOL_BIND_WORKSPACE,
                operatorName, null,
                Map.of("workspaceId", workspaceId, "workspaceName", workspaceName));

        eventPublisher.publishEvent(
                ToolChangeEvent.ofWorkspace(this, workspaceId, "bindToWorkspace: toolId=" + toolId));

        log.info("[ToolBindingService] 工具关联到 workspace: toolId={}, workspaceId={}", toolId, workspaceId);
    }

    /**
     * 将工具关联到 character（仅该 Character 专属可使用）。
     *
     * @param toolId        工具 ID（必须已存在）
     * @param characterId   目标 character ID
     * @param characterName character 名称（用于日志展示）
     * @param operatorId    操作人 ID
     * @param operatorName  操作人名称
     * @throws IllegalArgumentException 如果工具不存在
     */
    public void bindToCharacter(String toolId, String characterId,
                                String characterName,
                                Long operatorId, String operatorName) {
        validateToolExists(toolId);
        assetAssociationService.createAssociation(
                AssociationType.TOOL_CHARACTER,
                ResourceType.TOOL, toolId,
                ResourceType.CHARACTER, characterId);

        String toolName = toolStore.findById(toolId).map(t -> t.getName()).orElse(toolId);
        actionLogService.log(toolId, toolName, ActionType.TOOL_BIND_CHARACTER,
                operatorName, null,
                Map.of("characterId", characterId, "characterName", characterName));

        eventPublisher.publishEvent(
                ToolChangeEvent.ofCharacter(this, characterId, "bindToCharacter: toolId=" + toolId));

        log.info("[ToolBindingService] 工具关联到 character: toolId={}, characterId={}", toolId, characterId);
    }

    // ── 解绑操作 ─────────────────────────────────────────────────────────

    /**
     * 将工具从指定 workspace 解除关联。
     *
     * @param toolId      工具 ID
     * @param workspaceId workspace ID
     * @param targetName  workspace 名称（用于日志展示）
     * @param operatorId  操作人 ID
     * @param operatorName 操作人名称
     */
    public void unbindFromWorkspace(String toolId, String workspaceId,
                                    String targetName, Long operatorId, String operatorName) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        assetAssociationService.removeAssociation(
                AssociationType.TOOL_WORKSPACE,
                ResourceType.TOOL, toolId,
                ResourceType.WORKSPACE, workspaceId);

        String toolName = toolStore.findById(toolId).map(t -> t.getName()).orElse(toolId);
        actionLogService.log(toolId, toolName, ActionType.TOOL_UNBIND,
                operatorName, null,
                Map.of("scopeType", "workspace", "workspaceId", workspaceId, "workspaceName", targetName));

        eventPublisher.publishEvent(
                ToolChangeEvent.ofWorkspace(this, workspaceId, "unbindFromWorkspace: toolId=" + toolId));

        log.info("[ToolBindingService] 工具从 workspace 解除关联: toolId={}, workspaceId={}", toolId, workspaceId);
    }

    /**
     * 将工具从指定 character 解除关联。
     *
     * @param toolId      工具 ID
     * @param characterId character ID
     * @param targetName  character 名称（用于日志展示）
     * @param operatorId  操作人 ID
     * @param operatorName 操作人名称
     */
    public void unbindFromCharacter(String toolId, String characterId,
                                    String targetName, Long operatorId, String operatorName) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        Objects.requireNonNull(characterId, "characterId must not be null");
        assetAssociationService.removeAssociation(
                AssociationType.TOOL_CHARACTER,
                ResourceType.TOOL, toolId,
                ResourceType.CHARACTER, characterId);

        String toolName = toolStore.findById(toolId).map(t -> t.getName()).orElse(toolId);
        actionLogService.log(toolId, toolName, ActionType.TOOL_UNBIND,
                operatorName, null,
                Map.of("scopeType", "character", "characterId", characterId, "characterName", targetName));

        eventPublisher.publishEvent(
                ToolChangeEvent.ofCharacter(this, characterId, "unbindFromCharacter: toolId=" + toolId));

        log.info("[ToolBindingService] 工具从 character 解除关联: toolId={}, characterId={}", toolId, characterId);
    }

    // ── 查询操作 ─────────────────────────────────────────────────────────

    /**
     * 查询指定 workspace 已关联的工具 ID 列表。
     *
     * @param workspaceId workspace ID
     * @return 工具 ID 列表
     */
    public List<String> getWorkspaceToolIds(String workspaceId) {
        Objects.requireNonNull(workspaceId, "workspaceId must not be null");
        return assetAssociationService.getToolsForWorkspace(workspaceId);
    }

    /**
     * 查询指定 character 已关联的工具 ID 列表。
     *
     * @param characterId character ID
     * @return 工具 ID 列表
     */
    public List<String> getCharacterToolIds(String characterId) {
        Objects.requireNonNull(characterId, "characterId must not be null");
        return assetAssociationService.getToolsForCharacter(characterId);
    }

    /**
     * 查询指定工具被关联到哪些 workspace 和 character（工具详情页"使用情况"）。
     *
     * @param toolId 工具 ID
     * @return 包含 workspaceIds 和 characterIds 两个 key 的结构化结果
     */
    public ToolAssociationSummary getToolAssociations(String toolId) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        List<String> workspaceIds = assetAssociationService.getWorkspacesForTool(toolId);
        List<String> characterIds = assetAssociationService.getCharactersForTool(toolId);
        return new ToolAssociationSummary(toolId, workspaceIds, characterIds);
    }

    /**
     * 检查工具是否已关联到指定 workspace。
     *
     * @param toolId      工具 ID
     * @param workspaceId workspace ID
     * @return true 表示已关联
     */
    public boolean isBindToWorkspace(String toolId, String workspaceId) {
        return assetAssociationService.exists(
                AssociationType.TOOL_WORKSPACE,
                ResourceType.TOOL, toolId,
                ResourceType.WORKSPACE, workspaceId);
    }

    /**
     * 检查工具是否已关联到指定 character。
     *
     * @param toolId      工具 ID
     * @param characterId character ID
     * @return true 表示已关联
     */
    public boolean isBindToCharacter(String toolId, String characterId) {
        return assetAssociationService.exists(
                AssociationType.TOOL_CHARACTER,
                ResourceType.TOOL, toolId,
                ResourceType.CHARACTER, characterId);
    }

    // ── 内部工具方法 ─────────────────────────────────────────────────────

    private void validateToolExists(String toolId) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        if (!toolStore.existsById(toolId)) {
            throw new IllegalArgumentException("Tool not found: " + toolId);
        }
    }

    /**
     * 工具关联汇总（工具被关联到哪些 workspace 和 character）。
     */
    public record ToolAssociationSummary(String toolId, List<String> workspaceIds, List<String> characterIds) {}
}

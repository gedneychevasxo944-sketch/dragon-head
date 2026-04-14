package org.dragon.tool.service;

import org.dragon.asset.service.AssetAssociationService;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.store.ToolStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 工具查询服务（管理面）。
 *
 * <p>提供工具管理页面所需的查询能力，包括：
 * <ul>
 *   <li>工具详情加载（按 ID 查找）</li>
 *   <li>关联关系查询（某工具被哪些 workspace/character 使用）</li>
 * </ul>
 */
@Service
public class ToolQueryService {

    private final ToolStore toolStore;
    private final AssetAssociationService assetAssociationService;

    public ToolQueryService(ToolStore toolStore,
                            AssetAssociationService assetAssociationService) {
        this.toolStore = Objects.requireNonNull(toolStore, "toolStore must not be null");
        this.assetAssociationService = Objects.requireNonNull(assetAssociationService, "assetAssociationService must not be null");
    }

    // ── 工具基本查询 ─────────────────────────────────────────────────────

    /**
     * 根据工具 ID 获取工具详情。
     *
     * @param toolId 工具 ID
     * @return 工具主记录（Optional）
     */
    public Optional<ToolDO> getToolById(String toolId) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        return toolStore.findById(toolId);
    }

    // ── 关联关系查询 ─────────────────────────────────────────────────────

    /**
     * 查询工具被关联到哪些 workspace（工具详情页"使用情况"）。
     *
     * @param toolId 工具 ID
     * @return workspace ID 列表
     */
    public List<String> getWorkspacesForTool(String toolId) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        return assetAssociationService.getWorkspacesForTool(toolId);
    }

    /**
     * 查询工具被关联到哪些 character（工具详情页"使用情况"）。
     *
     * @param toolId 工具 ID
     * @return character ID 列表
     */
    public List<String> getCharactersForTool(String toolId) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        return assetAssociationService.getCharactersForTool(toolId);
    }

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
}

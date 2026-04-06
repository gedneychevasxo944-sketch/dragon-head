package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.CollaboratorService;
import org.dragon.permission.service.PermissionService;
import org.dragon.tools.ToolRegistry;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ToolApplication Tool 模块应用服务
 *
 * <p>对应前端 /tools 页面，聚合工具的查询、注册、版本管理等业务逻辑。
 * 核心操作委托给 ToolRegistry 处理。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolApplication {

    private final ToolRegistry toolRegistry;
    private final PermissionService permissionService;
    private final CollaboratorService collaboratorService;

    /**
     * 分页获取工具列表。
     */
    public PageResponse<Map<String, Object>> listTools(int page, int pageSize, String search,
            String visibility, String toolType, String runtimeStatus, String category) {
        // 获取所有工具（包括运行时和元数据）
        List<Map<String, Object>> all = toolRegistry.listAllWithMetadata();

        // 按用户可见性过滤
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.TOOL, userId);

        List<Map<String, Object>> filtered = all.stream()
                .filter(t -> {
                    String name = (String) t.get("name");
                    String toolVisibility = (String) t.get("visibility");

                    // 可见性过滤：如果是公开的则对所有用户可见；否则需要检查权限
                    if (!"PUBLIC".equals(toolVisibility)) {
                        if (visibleIds == null || visibleIds.isEmpty() || !visibleIds.contains(name)) {
                            return false;
                        }
                    }

                    // 搜索过滤（search）
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = name != null && name.toLowerCase().contains(s);
                        String desc = (String) t.get("description");
                        boolean descMatch = desc != null && desc.toLowerCase().contains(s);
                        if (!nameMatch && !descMatch) {
                            return false;
                        }
                    }

                    // 可见性过滤（visibility）
                    if (visibility != null && !visibility.isBlank() && !"all".equals(visibility)) {
                        if (!visibility.equals(toolVisibility)) {
                            return false;
                        }
                    }

                    // 类型过滤（toolType）
                    if (toolType != null && !toolType.isBlank() && !"all".equals(toolType)) {
                        String tType = (String) t.get("toolType");
                        if (!toolType.equals(tType)) {
                            return false;
                        }
                    }

                    // 运行状态过滤（runtimeStatus）
                    if (runtimeStatus != null && !runtimeStatus.isBlank() && !"all".equals(runtimeStatus)) {
                        String rState = (String) t.get("runtimeState");
                        if (!runtimeStatus.equals(rState)) {
                            return false;
                        }
                    }

                    // 分类过滤（category）
                    if (category != null && !category.isBlank() && !"all".equals(category)) {
                        String cat = (String) t.get("category");
                        if (!category.equals(cat)) {
                            return false;
                        }
                    }

                    return true;
                })
                // 按更新时间降序排列
                .sorted((t1, t2) -> {
                    Long updatedAt1 = t1.get("updatedAt") != null ? ((Number) t1.get("updatedAt")).longValue() : 0L;
                    Long updatedAt2 = t2.get("updatedAt") != null ? ((Number) t2.get("updatedAt")).longValue() : 0L;
                    return Long.compare(updatedAt2, updatedAt1);
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Map<String, Object>> pageData = fromIndex >= filtered.size() ? List.of()
                : filtered.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 获取工具详情。
     */
    public Optional<Map<String, Object>> getTool(String toolId) {
        return toolRegistry.findTool(toolId);
    }

    /**
     * 创建工具。
     */
    public Map<String, Object> createTool(Map<String, Object> toolData) {
        Map<String, Object> result = toolRegistry.create(toolData);

        // 创建成功后，将当前用户添加为该工具的所有者
        if (Boolean.TRUE.equals(result.get("success"))) {
            String toolName = (String) result.get("name");
            if (toolName != null) {
                try {
                    Long userId = Long.parseLong(UserUtils.getUserId());
                    collaboratorService.addOwnerDirectly(ResourceType.TOOL, toolName, userId);
                    log.info("[ToolApplication] Added owner for tool: {}", toolName);
                } catch (Exception e) {
                    log.warn("[ToolApplication] Failed to add owner for tool: {}", toolName, e);
                }
            }
        }

        return result;
    }

    /**
     * 更新工具。
     */
    public Map<String, Object> updateTool(String toolId, Map<String, Object> toolData) {
        return toolRegistry.update(toolId, toolData);
    }

    /**
     * 删除工具。
     */
    public Map<String, Object> deleteTool(String toolId) {
        return toolRegistry.delete(toolId);
    }
}
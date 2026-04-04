package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.tools.AgentTool;
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
 * 当前 Tool 系统为运行时注册机制，没有持久化数据库，
 * 部分 API_SPEC 中的字段（如版本历史）需后续扩展。
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

    // ==================== Tool 查询 ====================

    /**
     * 分页获取工具列表。
     *
     * @param page     页码
     * @param pageSize 每页数量
     * @param search   搜索关键词
     * @return 分页结果
     */
    public PageResponse<Map<String, Object>> listTools(int page, int pageSize, String search) {
        List<AgentTool> all = toolRegistry.listAll();

        // 按用户可见性过滤
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.TOOL, userId);

        List<Map<String, Object>> filtered = all.stream()
                .filter(t -> {
                    // 可见性过滤
                    if (visibleIds != null && !visibleIds.isEmpty() && !visibleIds.contains(t.getName())) {
                        return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = t.getName() != null && t.getName().toLowerCase().contains(s);
                        boolean descMatch = t.getDescription() != null && t.getDescription().toLowerCase().contains(s);
                        return nameMatch || descMatch;
                    }
                    return true;
                })
                .map(this::toToolMap)
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
     *
     * @param toolId 工具名称（Tool 以 name 为 ID）
     * @return 工具信息
     */
    public Optional<Map<String, Object>> getTool(String toolId) {
        return toolRegistry.get(toolId).map(this::toToolMap);
    }

    // ==================== 内部工具 ====================

    private Map<String, Object> toToolMap(AgentTool tool) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tool.getName());
        map.put("name", tool.getName());
        map.put("description", tool.getDescription() != null ? tool.getDescription() : "");
        map.put("enabled", true);
        map.put("toolType", "FUNCTION");
        map.put("invocationType", "native");
        map.put("visibility", "PUBLIC");
        map.put("category", "builtin");
        map.put("tags", List.of());
        map.put("creator", "system");
        map.put("parameters", tool.getParameterSchema() != null ? tool.getParameterSchema() : Map.of());
        map.put("runtimeState", "ACTIVE");
        return map;
    }
}

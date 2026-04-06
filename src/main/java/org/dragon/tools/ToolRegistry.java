package org.dragon.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dragon.store.StoreFactory;
import org.dragon.tools.store.ToolStore;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 工具注册中心
 *
 * <p>
 * 负责管理所有可用的 Agent 工具，包括工具的注册、创建、查找、删除、过滤等功能。
 * 支持两种模式：
 * 1. 运行时注册：直接注册 AgentTool 实例到内存
 * 2. 元数据模式：从数据库加载工具元数据（未实现运行时执行的工具）
 * </p>
 */
@Slf4j
@Component
public class ToolRegistry {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToolStore toolStore;

    /** 工具名称到工具实例的映射（运行时内存缓存） */
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry(StoreFactory storeFactory) {
        this.toolStore = storeFactory.get(ToolStore.class);
    }

    // ==================== 运行时注册 ====================

    /**
     * 注册一个工具（运行时）。如果已存在同名工具，则覆盖。
     *
     * @param tool 要注册的工具
     */
    public void register(AgentTool tool) {
        tools.put(tool.getName(), tool);

        // 持久化工具元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", tool.getName());
        metadata.put("description", tool.getDescription());
        try {
            metadata.put("parameterSchema", objectMapper.writeValueAsString(tool.getParameterSchema()));
        } catch (JsonProcessingException e) {
            metadata.put("parameterSchema", "{}");
        }
        metadata.put("enabled", true);
        metadata.put("runtimeState", "ACTIVE");
        toolStore.save(metadata);

        log.debug("Registered tool: {}", tool.getName());
    }

    /**
     * 批量注册工具。如果工具名称已存在，则跳过。
     */
    public void registerAll(Collection<AgentTool> toolList) {
        for (AgentTool tool : toolList) {
            if (!tools.containsKey(tool.getName())) {
                register(tool);
            }
        }
    }

    /**
     * 根据名称获取运行时工具。
     */
    public Optional<AgentTool> get(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * 获取所有已注册的工具列表（运行时）。
     */
    public List<AgentTool> listAll() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 返回已注册工具的数量。
     */
    public int size() {
        return tools.size();
    }

    // ==================== 工具元数据 CRUD ====================

    /**
     * 创建工具（仅保存元数据，不注册到运行时）
     *
     * @param toolData 工具数据
     * @return 创建结果
     */
    public Map<String, Object> create(Map<String, Object> toolData) {
        String name = (String) toolData.get("name");

        if (name == null || name.isBlank()) {
            return Map.of("success", false, "error", "工具名称不能为空");
        }

        // 检查是否已存在（运行时或元数据）
        if (tools.containsKey(name) || toolStore.exists(name)) {
            return Map.of("success", false, "error", "工具已存在: " + name);
        }

        try {
            // 构建完整的元数据
            Map<String, Object> metadata = buildMetadata(toolData);
            metadata.put("runtimeState", "NOT_REGISTERED");

            // 保存到数据库
            toolStore.save(metadata);

            log.info("Created tool: {}", name);
            return Map.of("success", true, "id", name, "name", name);
        } catch (Exception e) {
            log.error("Failed to create tool: {}", name, e);
            return Map.of("success", false, "error", "创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新工具元数据
     *
     * @param name     工具名称
     * @param toolData 更新的数据
     * @return 更新结果
     */
    public Map<String, Object> update(String name, Map<String, Object> toolData) {
        // 检查是否存在
        Optional<Map<String, Object>> existing = toolStore.findByName(name);
        if (existing.isEmpty()) {
            return Map.of("success", false, "error", "工具不存在: " + name);
        }

        try {
            // 合并更新
            Map<String, Object> metadata = existing.get();
            metadata.put("updatedAt", System.currentTimeMillis());

            // 更新字段
            if (toolData.containsKey("description")) {
                metadata.put("description", toolData.get("description"));
            }
            if (toolData.containsKey("definition")) {
                metadata.put("definition", toolData.get("definition"));
            }
            if (toolData.containsKey("category")) {
                metadata.put("category", toolData.get("category"));
            }
            if (toolData.containsKey("visibility")) {
                metadata.put("visibility", toolData.get("visibility"));
            }
            if (toolData.containsKey("tags")) {
                metadata.put("tags", toolData.get("tags"));
            }
            if (toolData.containsKey("enabled")) {
                metadata.put("enabled", toolData.get("enabled"));
            }

            // 保存更新
            toolStore.update(metadata);

            log.info("Updated tool: {}", name);
            return Map.of("success", true, "id", name, "name", name);
        } catch (Exception e) {
            log.error("Failed to update tool: {}", name, e);
            return Map.of("success", false, "error", "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除工具（从数据库和内存中移除）
     *
     * @param name 工具名称
     * @return 删除结果
     */
    public Map<String, Object> delete(String name) {
        // 检查是否存在
        if (!tools.containsKey(name) && !toolStore.exists(name)) {
            return Map.of("success", false, "error", "工具不存在: " + name);
        }

        try {
            // 从运行时移除
            tools.remove(name);

            // 从数据库删除
            toolStore.delete(name);

            log.info("Deleted tool: {}", name);
            return Map.of("success", true);
        } catch (Exception e) {
            log.error("Failed to delete tool: {}", name, e);
            return Map.of("success", false, "error", "删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有工具元数据（包括未注册到运行时的）
     */
    public List<Map<String, Object>> listAllMetadata() {
        return toolStore.findAll();
    }

    /**
     * 根据名称获取工具元数据
     */
    public Optional<Map<String, Object>> getMetadata(String name) {
        return toolStore.findByName(name);
    }

    /**
     * 根据名称获取工具（优先运行时，其次元数据）
     */
    public Optional<Map<String, Object>> findTool(String name) {
        // 优先从运行时获取
        Optional<AgentTool> runtimeTool = get(name);
        if (runtimeTool.isPresent()) {
            return Optional.of(toolToMap(runtimeTool.get()));
        }

        // 其次从元数据获取
        return getMetadata(name).map(this::enrichMetadata);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建工具元数据
     */
    private Map<String, Object> buildMetadata(Map<String, Object> toolData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", toolData.get("name"));
        metadata.put("description", toolData.getOrDefault("description", ""));
        metadata.put("toolType", toolData.getOrDefault("toolType", "FUNCTION"));
        metadata.put("invocationType", toolData.getOrDefault("invocationType", "native"));
        metadata.put("definition", toolData.getOrDefault("definition", ""));
        metadata.put("category", toolData.getOrDefault("category", "custom"));
        metadata.put("visibility", toolData.getOrDefault("visibility", "PRIVATE"));
        metadata.put("tags", toolData.getOrDefault("tags", List.of()));
        metadata.put("enabled", toolData.getOrDefault("enabled", true));
        metadata.put("createdAt", System.currentTimeMillis());
        metadata.put("updatedAt", System.currentTimeMillis());

        // 处理 parameters
        if (toolData.containsKey("parameters")) {
            try {
                metadata.put("parameters", objectMapper.writeValueAsString(toolData.get("parameters")));
            } catch (JsonProcessingException e) {
                metadata.put("parameters", "[]");
            }
        }

        return metadata;
    }

    /**
     * 将 AgentTool 转换为 Map
     */
    private Map<String, Object> toolToMap(AgentTool tool) {
        Map<String, Object> map = new LinkedHashMap<>();
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
        map.put("currentVersion", "1.0.0");
        map.put("parameters", tool.getParameterSchema() != null ? tool.getParameterSchema() : Map.of());
        map.put("runtimeState", "ACTIVE");
        return map;
    }

    /**
     * 丰富元数据（补充缺失字段）
     */
    private Map<String, Object> enrichMetadata(Map<String, Object> metadata) {
        Map<String, Object> enriched = new LinkedHashMap<>(metadata);
        enriched.putIfAbsent("id", metadata.get("name"));
        enriched.putIfAbsent("enabled", true);
        enriched.putIfAbsent("toolType", "FUNCTION");
        enriched.putIfAbsent("invocationType", "native");
        enriched.putIfAbsent("visibility", "PRIVATE");
        enriched.putIfAbsent("category", "custom");
        enriched.putIfAbsent("tags", List.of());
        enriched.putIfAbsent("creator", "current_user");
        enriched.putIfAbsent("currentVersion", "1.0.0");

        // 返回值定义
        Map<String, Object> returnValue = new HashMap<>();
        returnValue.put("type", "object");
        returnValue.put("description", "工具执行结果");
        enriched.putIfAbsent("returnValue", returnValue);

        enriched.putIfAbsent("dependencies", List.of());
        enriched.putIfAbsent("versions", List.of());
        enriched.putIfAbsent("skillBindings", List.of());
        enriched.putIfAbsent("characterBindings", List.of());

        long now = System.currentTimeMillis();
        enriched.putIfAbsent("createdAt", now);
        enriched.putIfAbsent("updatedAt", now);

        return enriched;
    }

    // ==================== 兼容性方法 ====================

    /**
     * 获取所有工具（运行时 + 元数据）
     * 注意：返回全部工具（包括启用和禁用），enabled 状态由调用方处理
     */
    public List<Map<String, Object>> listAllWithMetadata() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 添加运行时工具
        for (AgentTool tool : tools.values()) {
            result.add(toolToMap(tool));
        }

        // 添加仅存在于元数据的工具（获取全部，不仅仅是启用的）
        List<Map<String, Object>> metadataList = toolStore.findAll();
        for (Map<String, Object> metadata : metadataList) {
            String name = (String) metadata.get("name");
            if (!tools.containsKey(name)) {
                result.add(enrichMetadata(metadata));
            }
        }

        return result;
    }

    /**
     * 将所有工具转换为 LLM 兼容的定义格式
     */
    public List<Map<String, Object>> toDefinitions() {
        return tools.values().stream()
                .map(tool -> {
                    Map<String, Object> def = new LinkedHashMap<>();
                    def.put("name", tool.getName());
                    def.put("description", tool.getDescription());
                    def.put("input_schema", tool.getParameterSchema());
                    return def;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据 ToolPolicy 过滤工具
     */
    public List<AgentTool> filterByPolicy(ToolPolicy policy) {
        if (policy == null || policy == ToolPolicy.ALLOW_ALL) {
            return listAll();
        }
        List<AgentTool> filtered = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            if (policy.isAllowed(tool.getName())) {
                filtered.add(tool);
            }
        }
        return filtered;
    }

    /**
     * 将工具转换为特定 provider 的格式
     */
    public List<Map<String, Object>> toProviderDefinitions(String provider) {
        return ToolDefinitionAdapter.toProviderFormat(listAll(), provider);
    }
}
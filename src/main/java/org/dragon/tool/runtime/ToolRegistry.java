package org.dragon.tool.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVersionStatus;
import org.dragon.tool.store.ToolStore;
import org.dragon.tool.store.ToolVersionStore;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Tool 注册中心（运行时）。
 *
 * <p>职责：
 * <ol>
 *   <li><b>可见性解析</b>：三源并集（内置 + workspace 绑定 + character 绑定）</li>
   *   <li><b>DO → Definition 转换</b>：通过 {@link #buildToolDefinition(ToolDO, ToolVersionDO)} 将 DB 实体
 *       聚合为纯运行时对象 {@link ToolDefinition}，解耦 Factory 和 Tool 实例对 DB 层的感知</li>
 *   <li><b>实例化</b>：通过 {@link ToolFactory} 将 {@link ToolDefinition} 构建为可调用的 {@link Tool} 实例</li>
 *   <li><b>L1 缓存</b>：Caffeine 按 characterId:workspaceId 缓存 Tool 实例列表，TTL=5分钟</li>
 * </ol>
 *
 * <p><b>可见性规则（三源并集）</b>：
 * <pre>
 * visible tools =
 *   ① 内置工具（builtin=true, status=ACTIVE）
 *   ∪ ② workspace 绑定的工具
 *   ∪ ③ character 绑定的工具
 *   → 按 toolId 去重（LinkedHashMap 保证内置工具优先）
 *   → 每个工具取其 publishedVersionId 对应的已发布版本
 *   → 通过 ToolFactory 构建 Tool 实例（singleton 实例可复用）
 * </pre>
 *
 * <p><b>ToolFactory 注入</b>：Registry 构造时传入 {@code List<ToolFactory>}，
 * 内部按 {@link ToolType} 建立路由 Map。
 *
 * <p><b>singleton vs prototype 实例</b>：
 * <ul>
 *   <li>{@link ToolFactory#isSingleton()} == true：实例随版本 key 缓存在 Caffeine 中，可复用；</li>
 *   <li>{@link ToolFactory#isSingleton()} == false（如 AGENT）：每次 {@link #findByName} 都重新构建实例，
 *       不缓存该实例（但 ToolDefinition 快照仍走缓存）。</li>
 * </ul>
 */
@Slf4j
@Component
public class ToolRegistry {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ToolStore toolStore;
    private final ToolVersionStore toolVersionStore;
    private final AssetAssociationService assetAssociationService;
    /** ToolType → ToolFactory 路由 Map */
    private final Map<ToolType, ToolFactory> factoryMap;

    /**
     * 缓存 key = "characterId:workspaceId"
     * 缓存 value = 当前上下文可见的 Tool 实例列表（仅 singleton 工具）
     */
    private Cache<String, List<Tool<JsonNode, ?>>> cache;
    
    /**
     * ToolDefinition 缓存（singleton + non-singleton 均包含）。
     * key = "characterId:workspaceId"
     */
    private Cache<String, List<ToolDefinition>> definitionCache;

    public ToolRegistry(ToolStore toolStore,
                        ToolVersionStore toolVersionStore,
                        AssetAssociationService assetAssociationService,
                        List<ToolFactory> factories) {
        this.toolStore = Objects.requireNonNull(toolStore, "toolStore must not be null");
        this.toolVersionStore = Objects.requireNonNull(toolVersionStore, "toolVersionStore must not be null");
        this.assetAssociationService = Objects.requireNonNull(assetAssociationService, "assetAssociationService must not be null");

        this.factoryMap = new EnumMap<>(ToolType.class);
        for (ToolFactory factory : factories) {
            factoryMap.put(factory.supportedType(), factory);
            log.info("[ToolRegistry] 注册 ToolFactory: type={}, factory={}",
                    factory.supportedType(), factory.getClass().getSimpleName());
        }
    }

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
        this.definitionCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
        log.info("[ToolRegistry] 初始化完成，已注册 ToolFactory 类型: {}", factoryMap.keySet());
    }

    // ── 主查询入口 ────────────────────────────────────────────────────

    /**
     * 获取指定上下文下可用的 Tool 实例列表（singleton 工具命中 Caffeine 缓存）。
     *
     * @param characterId 当前执行的 Character ID
     * @param workspaceId Character 所属的 Workspace ID
     * @return 当前上下文可见的 Tool 实例列表
     */
    public List<Tool<JsonNode, ?>> getTools(String characterId, String workspaceId) {
        String cacheKey = buildCacheKey(characterId, workspaceId);
        return cache.get(cacheKey, key -> buildToolInstances(characterId, workspaceId));
    }

    /**
     * 获取指定上下文下可见的 ToolDefinition 列表（Caffeine 缓存，TTL=5分钟）。
     *
     * <p>供 {@link #buildToolDeclarations} 使用：
     * 组装工具声明时直接从 ToolDefinition 提取 name/description/parameters/requiredParams/aliases，
     * 不依赖 Tool 实例，schema 来源统一为 DB（{@code tool_versions.parameters}）。
     *
     * @param characterId 当前执行的 Character ID
     * @param workspaceId Character 所属的 Workspace ID
     * @return 当前上下文可见的 ToolDefinition 列表（singleton + non-singleton 合并）
     */
    public List<ToolDefinition> getDefinitions(String characterId, String workspaceId) {
        String cacheKey = buildCacheKey(characterId, workspaceId);
        return definitionCache.get(cacheKey, key -> loadAndMergeDefinitions(characterId, workspaceId));
    }

    /**
     * 为当前 Character 构建工具声明列表，格式符合 {@link org.dragon.agent.llm.LLMRequest#getTools()} 约定。
     *
     * <p>每个 Map 的结构：
     * <pre>
     * {
     *   "name":        工具名称,
     *   "description": 工具描述,
     *   "input_schema": {
     *     "type":       "object",
     *     "properties": { 参数名: {type, description, ...}, ... },
     *     "required":   [必填参数名, ...]    // 有必填参数时才出现
     *   }
     * }
     * </pre>
     *
     * <p>各 {@link org.dragon.agent.llm.caller.LLMCaller} 实现会从 Map 中读取
     * {@code name}、{@code description}、{@code input_schema} 字段，组装为厂商格式（OpenAI / Kimi / Minimax 等）。
     *
     * @param characterId 当前执行的 Character ID
     * @param workspaceId Character 所属的 Workspace ID
     * @return LLMRequest.tools 格式的工具声明列表
     */
    public List<Map<String, Object>> buildToolDeclarations(String characterId, String workspaceId) {
        List<ToolDefinition> definitions = getDefinitions(characterId, workspaceId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolDefinition definition : definitions) {
            try {
                Map<String, Object> decl = buildToolDeclarationMap(definition);
                if (decl != null) result.add(decl);
            } catch (Exception e) {
                log.warn("[ToolRegistry] 工具声明构建失败: toolName={}, error={}",
                        definition.getName(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * 按名称查找 Tool 实例（含别名支持）。
     *
     * <p>对于 {@code isSingleton=false} 的工具（如 AGENT），每次调用都会重新创建实例，
     * 不从缓存读取。
     *
     * @param characterId 当前执行的 Character ID
     * @param workspaceId Character 所属的 Workspace ID
     * @param name        工具名称或别名
     * @return 匹配的 Tool 实例（Optional，不存在时为 empty）
     */
    public Optional<Tool<JsonNode, ?>> findByName(String characterId, String workspaceId, String name) {
        if (name == null) return Optional.empty();

        // 先尝试从 singleton 缓存中查找
        Optional<Tool<JsonNode, ?>> fromCache = getTools(characterId, workspaceId).stream()
                .filter(tool -> matchesName(tool, name))
                .findFirst();
        if (fromCache.isPresent()) {
            return fromCache;
        }

        // 再尝试从 non-singleton Definition 缓存中查找，每次重建实例
        return findNonSingletonByName(characterId, workspaceId, name);
    }

    // ── 缓存失效 ─────────────────────────────────────────────────────

    /**
     * 监听 ToolChangeEvent，精确或全量失效缓存。
     */
    @EventListener
    public void onToolChange(ToolChangeEvent event) {
        if (event.isGlobalEvict()) {
            log.info("[ToolRegistry] 全量缓存失效，原因: {}", event.getReason());
            cache.invalidateAll();
            definitionCache.invalidateAll();
            return;
        }

        int evicted = 0;
        for (String characterId : event.getAffectedCharacterIds()) {
            List<String> keys = cache.asMap().keySet().stream()
                    .filter(k -> k.startsWith(characterId + ":"))
                    .collect(Collectors.toList());
            cache.invalidateAll(keys);
            definitionCache.invalidateAll(keys);
            evicted += keys.size();
        }
        for (String workspaceId : event.getAffectedWorkspaceIds()) {
            List<String> keys = cache.asMap().keySet().stream()
                    .filter(k -> k.endsWith(":" + workspaceId))
                    .collect(Collectors.toList());
            cache.invalidateAll(keys);
            definitionCache.invalidateAll(keys);
            evicted += keys.size();
        }
        log.info("[ToolRegistry] 缓存失效 {} 条，原因: {}", evicted, event.getReason());
    }

    /**
     * 主动全量失效（供 MCP Server 重连等场景调用）。
     */
    public void invalidateAll() {
        cache.invalidateAll();
        definitionCache.invalidateAll();
        log.info("[ToolRegistry] 主动全量缓存失效");
    }

    // ── 私有：实例构建 ───────────────────────────────────────────────

    /**
     * cache miss 时调用：三源并集加载，转换为 ToolDefinition，用 ToolFactory 构建 Tool 实例。
     * 只有 isSingleton=true 的 Tool 才放入 singleton 缓存；non-singleton 的 ToolDefinition 快照写 definitionCache。
     */
    private List<Tool<JsonNode, ?>> buildToolInstances(String characterId, String workspaceId) {
        log.debug("[ToolRegistry] cache miss，重新加载 key={}", buildCacheKey(characterId, workspaceId));

        List<ToolDefinition> definitions = loadAndMergeDefinitions(characterId, workspaceId);

        List<Tool<JsonNode, ?>> singletonTools = new ArrayList<>();
        List<ToolDefinition> nonSingletonDefinitions = new ArrayList<>();

        for (ToolDefinition definition : definitions) {
            ToolFactory factory = factoryMap.get(definition.getToolType());
            if (factory == null) {
                log.warn("[ToolRegistry] 无 ToolFactory 支持类型 {}，跳过工具: toolId={}",
                        definition.getToolType(), definition.getToolId());
                continue;
            }

            if (factory.isSingleton()) {
                try {
                    Tool<JsonNode, ?> tool = factory.create(definition);
                    singletonTools.add(tool);
                } catch (Exception e) {
                    log.warn("[ToolRegistry] 构建 Tool 实例失败: toolId={}, error={}",
                            definition.getToolId(), e.getMessage());
                }
            } else {
                nonSingletonDefinitions.add(definition);
            }
        }

        // non-singleton ToolDefinition 快照写入 definitionCache
        String cacheKey = buildCacheKey(characterId, workspaceId);
        definitionCache.put(cacheKey, nonSingletonDefinitions);

        log.debug("[ToolRegistry] 构建完成: singleton={}, nonSingleton={}",
                singletonTools.size(), nonSingletonDefinitions.size());
        return singletonTools;
    }

    /**
     * 在 non-singleton Definition 缓存中查找，每次重新用 Factory 构建实例。
     */
    private Optional<Tool<JsonNode, ?>> findNonSingletonByName(String characterId,
                                                                String workspaceId,
                                                                String name) {
        String cacheKey = buildCacheKey(characterId, workspaceId);
        List<ToolDefinition> nonSingletonDefinitions = definitionCache.getIfPresent(cacheKey);
        if (nonSingletonDefinitions == null || nonSingletonDefinitions.isEmpty()) {
            return Optional.empty();
        }

        return nonSingletonDefinitions.stream()
                .filter(r -> {
                    ToolFactory factory = factoryMap.get(r.getToolType());
                    return factory != null && !factory.isSingleton();
                })
                .filter(r -> matchesDefinitionName(r, name))
                .findFirst()
                .flatMap(definition -> {
                    ToolFactory factory = factoryMap.get(definition.getToolType());
                    if (factory == null) return Optional.empty();
                    try {
                        return Optional.of(factory.create(definition));
                    } catch (Exception e) {
                        log.warn("[ToolRegistry] 构建 non-singleton Tool 失败: toolId={}, error={}",
                                definition.getToolId(), e.getMessage());
                        return Optional.empty();
                    }
                });
    }

    // ── 私有：三源并集加载 & DO→Definition 转换 ────────────────────────

    /**
     * 三源并集加载，转换为 {@link ToolDefinition} 列表。
     *
     * <p>转换在此方法内集中完成，Factory 和 Tool 实例类无需感知 DB 层。
     */
    private List<ToolDefinition> loadAndMergeDefinitions(String characterId, String workspaceId) {
        // LinkedHashMap 保证 builtin → workspace → character 的优先级顺序（按 toolId 去重）
        Map<String, ToolDefinition> mergedMap = new LinkedHashMap<>();

        // ① 内置工具：builtin=true, status=ACTIVE
        List<ToolDO> builtinTools = toolStore.findAllBuiltin();
        for (ToolDO tool : builtinTools) {
            resolveToDefinition(tool).ifPresent(r -> mergedMap.put(tool.getId(), r));
        }
        log.debug("[ToolRegistry] 内置 Tool {} 个", builtinTools.size());

        // ② workspace 关联的工具
        List<String> workspaceToolIds = workspaceId != null
                ? assetAssociationService.getToolsForWorkspace(workspaceId)
                : List.of();
        if (!workspaceToolIds.isEmpty()) {
            resolveDefinitionsByToolIds(workspaceToolIds)
                    .forEach(r -> mergedMap.putIfAbsent(r.getToolId(), r));
        }
        log.debug("[ToolRegistry] workspace 关联 Tool {} 个", workspaceToolIds.size());

        // ③ character 关联的工具
        List<String> characterToolIds = characterId != null
                ? assetAssociationService.getToolsForCharacter(characterId)
                : List.of();
        if (!characterToolIds.isEmpty()) {
            resolveDefinitionsByToolIds(characterToolIds)
                    .forEach(r -> mergedMap.putIfAbsent(r.getToolId(), r));
        }
        log.debug("[ToolRegistry] character 关联 Tool {} 个", characterToolIds.size());

        List<ToolDefinition> result = new ArrayList<>(mergedMap.values());
        log.debug("[ToolRegistry] 合并后可见 Tool {} 个", result.size());
        return result;
    }

    /**
     * 解析单个 ToolDO 到 ToolDefinition（通过已发布版本）。
     */
    private Optional<ToolDefinition> resolveToDefinition(ToolDO tool) {
        return resolvePublishedVersion(tool)
                .map(version -> buildToolDefinition(tool, version));
    }

    /**
     * 批量解析 toolId 列表到 ToolDefinition 列表。
     */
    private List<ToolDefinition> resolveDefinitionsByToolIds(List<String> toolIds) {
        return toolStore.findByIds(toolIds).stream()
                .filter(tool -> tool.getStatus() == ToolStatus.ACTIVE && !tool.isDeleted())
                .flatMap(tool -> resolveToDefinition(tool).stream())
                .collect(Collectors.toList());
    }

    /**
     * 查找 ToolDO 对应的已发布 ToolVersionDO。
     */
    private Optional<ToolVersionDO> resolvePublishedVersion(ToolDO tool) {
        if (tool.getPublishedVersionId() != null) {
            return toolVersionStore.findById(tool.getPublishedVersionId())
                    .filter(v -> v.getStatus() == ToolVersionStatus.PUBLISHED);
        }
        return toolVersionStore.findPublishedByToolId(tool.getId());
    }

    // ── 核心转换方法 ─────────────────────────────────────────────────

    /**
     * 从 {@link ToolDO} + {@link ToolVersionDO} 聚合构建 {@link ToolDefinition}。
     *
     * <p>对标 {@code SkillRegistry.buildDefinitionFromVersion()}，是 DB 层到 Definition 层的唯一边界：
     * <ul>
     *   <li>过滤掉管理端字段（editorId、editorName、releaseNote、status、createdAt、publishedAt 等）</li>
     *   <li>别名字段从 JSON 字符串反序列化为 {@code List<String>}</li>
     *   <li>标签字段从 ToolDO 的 JSON 字符串反序列化为 {@code List<String>}</li>
     * </ul>
     *
     * @param tool    工具主记录（提供 toolId、toolType、tags）
     * @param version 已发布版本（提供 LLM 声明 + executionConfig + storageInfo）
     * @return 运行时快照
     */
    private ToolDefinition buildToolDefinition(ToolDO tool, ToolVersionDO version) {
        return ToolDefinition.builder()
                .toolId(tool.getId())
                .toolType(version.getToolType() != null ? version.getToolType() : tool.getToolType())
                .version(version.getVersion() != null ? version.getVersion() : 0)
                .name(version.getName())
                .description(version.getDescription())
                .parameters(version.getParameters())
                .requiredParams(version.getRequiredParams())
                .aliases(parseJsonList(version.getAliases()))
                .executionConfig(version.getExecutionConfig())
                .storageType(version.getStorageType() != null ? version.getStorageType().name() : null)
                .storageInfo(version.getStorageInfo())
                .tags(parseJsonList(tool.getTags()))
                .build();
    }

    // ── 私有：名称匹配 ───────────────────────────────────────────────

    /** 匹配 Tool 实例的名称和别名 */
    private boolean matchesName(Tool<JsonNode, ?> tool, String name) {
        if (tool.getName().equals(name)) return true;
        List<String> aliases = tool.getAliases();
        return aliases != null && aliases.contains(name);
    }

    /** 匹配 ToolDefinition 的名称和别名（用于 non-singleton 查找） */
    private boolean matchesDefinitionName(ToolDefinition definition, String name) {
        if (name.equals(definition.getName())) return true;
        List<String> aliases = definition.getAliases();
        return aliases != null && aliases.contains(name);
    }

    // ── 私有：声明组装 ───────────────────────────────────────────────

    /**
     * 将单个 {@link ToolDefinition} 组装为 {@link org.dragon.agent.llm.LLMRequest#getTools()} 约定格式。
     *
     * <p>返回格式：
     * <pre>
     * {
     *   "name": "...",
     *   "description": "...",
     *   "input_schema": {
     *     "type": "object",
     *     "properties": { "param": {"type": "string", "description": "..."}, ... },
     *     "required": ["param1", ...]   // 仅当 requiredParams 非空时出现
     *   }
     * }
     * </pre>
     *
     * @return 组装好的 Map，name 或 description 为空时返回 null（跳过该工具）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildToolDeclarationMap(ToolDefinition definition) {
        if (definition.getName() == null || definition.getName().isBlank()) return null;
        if (definition.getDescription() == null || definition.getDescription().isBlank()) return null;

        Map<String, Object> decl = new java.util.LinkedHashMap<>();
        decl.put("name", definition.getName());
        decl.put("description", definition.getDescription());

        // 组装 input_schema
        Map<String, Object> inputSchema = new java.util.LinkedHashMap<>();
        inputSchema.put("type", "object");

        // 解析 parameters（JSON → Map<String, Object>）
        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        if (definition.getParameters() != null && !definition.getParameters().isBlank()) {
            try {
                Map<String, Object> parsed = OBJECT_MAPPER.readValue(
                        definition.getParameters(), new TypeReference<Map<String, Object>>() {});
                properties.putAll(parsed);
            } catch (Exception e) {
                log.warn("[ToolRegistry] 解析 parameters 失败: toolId={}, error={}",
                        definition.getToolId(), e.getMessage());
            }
        }
        inputSchema.put("properties", properties);

        // 解析 requiredParams（JSON 数组 → List<String>）
        if (definition.getRequiredParams() != null && !definition.getRequiredParams().isBlank()) {
            try {
                List<String> required = OBJECT_MAPPER.readValue(
                        definition.getRequiredParams(), new TypeReference<List<String>>() {});
                if (required != null && !required.isEmpty()) {
                    inputSchema.put("required", required);
                }
            } catch (Exception e) {
                log.warn("[ToolRegistry] 解析 requiredParams 失败: toolId={}, error={}",
                        definition.getToolId(), e.getMessage());
            }
        }

        decl.put("input_schema", inputSchema);
        return decl;
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    private String buildCacheKey(String characterId, String workspaceId) {
        return (characterId != null ? characterId : "")
                + ":"
                + (workspaceId != null ? workspaceId : "");
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}

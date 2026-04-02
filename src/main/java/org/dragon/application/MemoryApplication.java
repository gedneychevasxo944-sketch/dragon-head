package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.memory.MemorySearchManager;
import org.dragon.memory.MemorySearchManagerFactory;
import org.dragon.memory.models.MemorySearchResult;
import org.dragon.memory.models.SearchOptions;
import org.dragon.memory.models.SyncRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * MemoryApplication Memory 模块应用服务
 *
 * <p>对应前端 /memory 页面，聚合记忆数据源、文件、片段、绑定关系、检索、配置等业务逻辑。
 * 当前记忆系统后端为 builtin/qmd 两种实现，部分数据源管理功能需数据库支持。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryApplication {

    private final MemorySearchManagerFactory memorySearchManagerFactory;

    /** 默认配置用于获取 MemorySearchManager */
    private static final Map<String, Object> DEFAULT_CONFIG = Map.of("backend", "builtin");

    private MemorySearchManager getManager() {
        return memorySearchManagerFactory.getMemorySearchManager(DEFAULT_CONFIG);
    }

    // ==================== 数据源管理（Source）====================

    /**
     * 获取数据源列表（占位，需要持久化层支持）。
     *
     * @param search     搜索关键词
     * @param sourceType 类型筛选
     * @param status     状态筛选
     * @return 数据源列表
     */
    public List<Map<String, Object>> listSources(String search, String sourceType, String status) {
        log.info("[MemoryApplication] listSources search={} sourceType={} status={}", search, sourceType, status);
        // 占位：需要数据库持久化层（MemorySourceStore）支持
        return List.of();
    }

    /**
     * 添加数据源（占位）。
     */
    public Map<String, Object> addSource(String title, String sourceType, String sourcePath,
                                         String backend, String provider) {
        Map<String, Object> source = new HashMap<>();
        source.put("id", UUID.randomUUID().toString());
        source.put("title", title);
        source.put("sourceType", sourceType);
        source.put("sourcePath", sourcePath);
        source.put("backend", backend);
        source.put("provider", provider);
        source.put("status", "active");
        source.put("enabled", true);
        log.info("[MemoryApplication] addSource title={}", title);
        return source;
    }

    /**
     * 获取数据源详情（占位）。
     */
    public Optional<Map<String, Object>> getSource(String sourceId) {
        return Optional.empty();
    }

    /**
     * 触发数据源同步。
     */
    public Map<String, Object> syncSource(String sourceId) {
        log.info("[MemoryApplication] syncSource sourceId={}", sourceId);
        try {
            // SyncRequest 没有 setSourceId 方法，使用 forceSync 便捷方法
            SyncRequest request = SyncRequest.forceSync("Manual sync for source: " + sourceId);
            getManager().sync(request);
            return Map.of("success", true, "sourceId", sourceId);
        } catch (Exception e) {
            log.error("[MemoryApplication] syncSource failed: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * 删除数据源（占位）。
     */
    public void deleteSource(String sourceId) {
        log.info("[MemoryApplication] deleteSource sourceId={}", sourceId);
    }

    // ==================== 记忆文件（File）====================

    /**
     * 获取记忆文件列表（占位）。
     */
    public PageResponse<Map<String, Object>> listFiles(int page, int pageSize, String sourceId,
                                                       String search, String syncStatus) {
        return PageResponse.of(List.of(), 0, page, pageSize);
    }

    /**
     * 获取记忆文件详情（占位）。
     */
    public Optional<Map<String, Object>> getFile(String fileId) {
        return Optional.empty();
    }

    // ==================== 记忆片段（Chunk）====================

    /**
     * 获取记忆片段列表（占位）。
     */
    public PageResponse<Map<String, Object>> listChunks(int page, int pageSize, String fileId,
                                                        String search, String indexedStatus) {
        return PageResponse.of(List.of(), 0, page, pageSize);
    }

    /**
     * 创建或编辑记忆片段（占位）。
     */
    public Map<String, Object> saveChunk(String chunkId, String fileId, String title,
                                         String content, String summary, List<String> tags) {
        Map<String, Object> chunk = new HashMap<>();
        chunk.put("id", chunkId != null ? chunkId : UUID.randomUUID().toString());
        chunk.put("fileId", fileId);
        chunk.put("title", title);
        chunk.put("content", content);
        chunk.put("summary", summary);
        chunk.put("tags", tags != null ? tags : List.of());
        chunk.put("indexedStatus", "pending");
        return chunk;
    }

    /**
     * 删除记忆片段（占位）。
     */
    public void deleteChunk(String chunkId) {
        log.info("[MemoryApplication] deleteChunk chunkId={}", chunkId);
    }

    // ==================== 绑定关系（Binding）====================

    /**
     * 获取绑定列表（占位）。
     */
    public List<Map<String, Object>> listBindings(String fileId, String targetType, String targetId) {
        return List.of();
    }

    /**
     * 创建绑定（占位）。
     */
    public Map<String, Object> createBinding(String fileId, String targetType, String targetId,
                                             String mountType, List<String> selectedChunkIds) {
        Map<String, Object> binding = new HashMap<>();
        binding.put("id", UUID.randomUUID().toString());
        binding.put("fileId", fileId);
        binding.put("targetType", targetType);
        binding.put("targetId", targetId);
        binding.put("mountType", mountType);
        binding.put("selectedChunkIds", selectedChunkIds != null ? selectedChunkIds : List.of());
        binding.put("mountedAt", java.time.LocalDateTime.now().toString());
        return binding;
    }

    /**
     * 删除绑定（占位）。
     */
    public void deleteBinding(String bindingId) {
        log.info("[MemoryApplication] deleteBinding bindingId={}", bindingId);
    }

    // ==================== 检索（Retrieval）====================

    /**
     * 向量/全文混合检索记忆。
     *
     * @param query     查询文本
     * @param scopeType 作用域类型
     * @param scopeId   作用域 ID
     * @param topK      返回条数
     * @param minScore  最低相关度
     * @return 检索结果列表
     */
    public List<Map<String, Object>> search(String query, String scopeType, String scopeId,
                                            int topK, double minScore) {
        try {
            SearchOptions options = new SearchOptions();
            options.setMaxResults(topK);
            options.setMinScore(minScore);

            List<MemorySearchResult> results = getManager().search(query, options);

            List<Map<String, Object>> list = new ArrayList<>();
            for (MemorySearchResult r : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", UUID.randomUUID().toString());
                item.put("path", r.getPath() != null ? r.getPath() : "");
                item.put("source", r.getSource() != null ? r.getSource() : "");
                item.put("score", r.getScore());
                item.put("snippet", r.getSnippet() != null ? r.getSnippet() : "");
                item.put("startLine", r.getStartLine());
                item.put("endLine", r.getEndLine());
                item.put("citation", r.getCitation());
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            log.error("[MemoryApplication] search failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== 记忆配置（Config）====================

    /**
     * 获取 Character 记忆配置（占位）。
     */
    public Map<String, Object> getCharacterMemoryConfig(String characterId) {
        Map<String, Object> config = new HashMap<>();
        config.put("characterId", characterId);
        config.put("enabled", true);
        config.put("privateMemoryEnabled", false);
        config.put("defaultSources", List.of());
        return config;
    }

    /**
     * 更新 Character 记忆配置（占位）。
     */
    public Map<String, Object> updateCharacterMemoryConfig(String characterId, Map<String, Object> config) {
        config.put("characterId", characterId);
        log.info("[MemoryApplication] updateCharacterMemoryConfig characterId={}", characterId);
        return config;
    }

    /**
     * 获取 Workspace 记忆配置（占位）。
     */
    public Map<String, Object> getWorkspaceMemoryConfig(String workspaceId) {
        Map<String, Object> config = new HashMap<>();
        config.put("workspaceId", workspaceId);
        config.put("enabled", true);
        config.put("backend", "builtin");
        config.put("provider", "default");
        config.put("model", "default");
        config.put("syncStrategy", "auto");
        return config;
    }

    /**
     * 更新 Workspace 记忆配置（占位）。
     */
    public Map<String, Object> updateWorkspaceMemoryConfig(String workspaceId, Map<String, Object> config) {
        config.put("workspaceId", workspaceId);
        log.info("[MemoryApplication] updateWorkspaceMemoryConfig workspaceId={}", workspaceId);
        return config;
    }

    /**
     * 获取记忆运行时状态。
     */
    public Map<String, Object> getRuntimeStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            MemorySearchManager manager = getManager();
            status.put("healthy", true);
            status.put("backend", "builtin");
        } catch (Exception e) {
            status.put("healthy", false);
            status.put("error", e.getMessage());
        }
        return status;
    }
}

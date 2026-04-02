package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.MemoryApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * MemoryController 记忆模块 API
 *
 * <p>对应前端 /memory 页面，包含数据源、文件、片段、绑定、检索、配置等接口。
 * Base URL: /api/v1/memory
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Memory", description = "记忆模块")
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MemoryController {

    private final MemoryApplication memoryApplication;

    // ==================== 16. Source（数据源）====================

    /**
     * 16.1 获取数据源列表
     * GET /api/v1/memory/sources
     */
    @Operation(summary = "获取记忆数据源列表")
    @GetMapping("/sources")
    public ApiResponse<List<Map<String, Object>>> listSources(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> sources = memoryApplication.listSources(search, sourceType, status);
        return ApiResponse.success(sources);
    }

    /**
     * 16.2 添加数据源
     * POST /api/v1/memory/sources
     */
    @Operation(summary = "添加记忆数据源")
    @PostMapping("/sources")
    public ApiResponse<Map<String, Object>> addSource(@RequestBody AddSourceRequest request) {
        Map<String, Object> source = memoryApplication.addSource(
                request.getTitle(),
                request.getSourceType(),
                request.getSourcePath(),
                request.getBackend(),
                request.getProvider());
        return ApiResponse.success(source);
    }

    /**
     * 16.3 获取数据源详情（含文件列表）
     * GET /api/v1/memory/sources/:sourceId
     */
    @Operation(summary = "获取数据源详情（含文件列表）")
    @GetMapping("/sources/{sourceId}")
    public ApiResponse<Map<String, Object>> getSource(@PathVariable String sourceId) {
        return memoryApplication.getSource(sourceId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Source not found: " + sourceId));
    }

    /**
     * 16.4 触发数据源同步
     * POST /api/v1/memory/sources/:sourceId/sync
     */
    @Operation(summary = "触发数据源同步")
    @PostMapping("/sources/{sourceId}/sync")
    public ApiResponse<Map<String, Object>> syncSource(@PathVariable String sourceId) {
        Map<String, Object> result = memoryApplication.syncSource(sourceId);
        return ApiResponse.success(result);
    }

    /**
     * 16.5 删除数据源
     * DELETE /api/v1/memory/sources/:sourceId
     */
    @Operation(summary = "删除数据源")
    @DeleteMapping("/sources/{sourceId}")
    public ApiResponse<Map<String, Object>> deleteSource(@PathVariable String sourceId) {
        memoryApplication.deleteSource(sourceId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 17. File（记忆文件）====================

    /**
     * 17.1 获取记忆文件列表
     * GET /api/v1/memory/files
     */
    @Operation(summary = "获取记忆文件列表")
    @GetMapping("/files")
    public ApiResponse<PageResponse<Map<String, Object>>> listFiles(
            @RequestParam(required = false) String sourceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String syncStatus,
            @RequestParam(required = false) String healthStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = memoryApplication.listFiles(
                page, pageSize, sourceId, search, syncStatus);
        return ApiResponse.success(result);
    }

    /**
     * 17.2 获取记忆文件详情
     * GET /api/v1/memory/files/:fileId
     */
    @Operation(summary = "获取记忆文件详情")
    @GetMapping("/files/{fileId}")
    public ApiResponse<Map<String, Object>> getFile(@PathVariable String fileId) {
        return memoryApplication.getFile(fileId)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "File not found: " + fileId));
    }

    // ==================== 18. Chunk（记忆片段）====================

    /**
     * 18.1 获取记忆片段列表
     * GET /api/v1/memory/chunks
     */
    @Operation(summary = "获取记忆片段列表")
    @GetMapping("/chunks")
    public ApiResponse<PageResponse<Map<String, Object>>> listChunks(
            @RequestParam(required = false) String fileId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String indexedStatus,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<Map<String, Object>> result = memoryApplication.listChunks(
                page, pageSize, fileId, search, indexedStatus);
        return ApiResponse.success(result);
    }

    /**
     * 18.2 创建记忆片段
     * POST /api/v1/memory/chunks
     */
    @Operation(summary = "创建记忆片段")
    @PostMapping("/chunks")
    public ApiResponse<Map<String, Object>> createChunk(@RequestBody ChunkRequest request) {
        Map<String, Object> chunk = memoryApplication.saveChunk(
                null,
                request.getFileId(),
                request.getTitle(),
                request.getContent(),
                request.getSummary(),
                request.getTags());
        return ApiResponse.success(chunk);
    }

    /**
     * 18.2 编辑记忆片段
     * PUT /api/v1/memory/chunks/:chunkId
     */
    @Operation(summary = "编辑记忆片段")
    @PutMapping("/chunks/{chunkId}")
    public ApiResponse<Map<String, Object>> updateChunk(
            @PathVariable String chunkId,
            @RequestBody ChunkRequest request) {
        Map<String, Object> chunk = memoryApplication.saveChunk(
                chunkId,
                request.getFileId(),
                request.getTitle(),
                request.getContent(),
                request.getSummary(),
                request.getTags());
        return ApiResponse.success(chunk);
    }

    /**
     * 18.3 批量打标签
     * POST /api/v1/memory/chunks/batch-tag
     */
    @Operation(summary = "批量打标签")
    @PostMapping("/chunks/batch-tag")
    public ApiResponse<Map<String, Object>> batchTag(@RequestBody Map<String, Object> request) {
        // 占位：批量标签操作
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 18.4 批量压缩（融合）片段
     * POST /api/v1/memory/chunks/compress
     */
    @Operation(summary = "批量压缩（融合）片段")
    @PostMapping("/chunks/compress")
    public ApiResponse<Map<String, Object>> compressChunks(@RequestBody Map<String, Object> request) {
        // 占位：片段融合操作
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 18.5 删除记忆片段
     * DELETE /api/v1/memory/chunks/:chunkId
     */
    @Operation(summary = "删除记忆片段")
    @DeleteMapping("/chunks/{chunkId}")
    public ApiResponse<Map<String, Object>> deleteChunk(@PathVariable String chunkId) {
        memoryApplication.deleteChunk(chunkId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 19. Binding（绑定关系）====================

    /**
     * 19.1 获取绑定列表
     * GET /api/v1/memory/bindings
     */
    @Operation(summary = "获取记忆绑定关系列表")
    @GetMapping("/bindings")
    public ApiResponse<List<Map<String, Object>>> listBindings(
            @RequestParam(required = false) String fileId,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId) {
        List<Map<String, Object>> bindings = memoryApplication.listBindings(fileId, targetType, targetId);
        return ApiResponse.success(bindings);
    }

    /**
     * 19.2 创建绑定
     * POST /api/v1/memory/bindings
     */
    @Operation(summary = "创建记忆绑定")
    @PostMapping("/bindings")
    public ApiResponse<Map<String, Object>> createBinding(@RequestBody CreateBindingRequest request) {
        Map<String, Object> binding = memoryApplication.createBinding(
                request.getFileId(),
                request.getTargetType(),
                request.getTargetId(),
                request.getMountType(),
                request.getSelectedChunkIds());
        return ApiResponse.success(binding);
    }

    /**
     * 19.3 删除绑定
     * DELETE /api/v1/memory/bindings/:bindingId
     */
    @Operation(summary = "删除记忆绑定")
    @DeleteMapping("/bindings/{bindingId}")
    public ApiResponse<Map<String, Object>> deleteBinding(@PathVariable String bindingId) {
        memoryApplication.deleteBinding(bindingId);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 20. Retrieval（检索）====================

    /**
     * 20.1 检索记忆
     * POST /api/v1/memory/retrieval/search
     */
    @Operation(summary = "向量/全文混合检索记忆")
    @PostMapping("/retrieval/search")
    public ApiResponse<List<Map<String, Object>>> search(@RequestBody SearchRequest request) {
        List<Map<String, Object>> results = memoryApplication.search(
                request.getQuery(),
                request.getScopeType(),
                request.getScopeId(),
                request.getTopK() != null ? request.getTopK() : 10,
                request.getMinScore() != null ? request.getMinScore() : 0.0);
        return ApiResponse.success(results);
    }

    // ==================== 21. Config（记忆配置）====================

    /**
     * 21.1 获取 Character 记忆配置
     * GET /api/v1/memory/config/character/:characterId
     */
    @Operation(summary = "获取 Character 记忆配置")
    @GetMapping("/config/character/{characterId}")
    public ApiResponse<Map<String, Object>> getCharacterMemoryConfig(
            @PathVariable String characterId) {
        Map<String, Object> config = memoryApplication.getCharacterMemoryConfig(characterId);
        return ApiResponse.success(config);
    }

    /**
     * 21.2 更新 Character 记忆配置
     * PUT /api/v1/memory/config/character/:characterId
     */
    @Operation(summary = "更新 Character 记忆配置")
    @PutMapping("/config/character/{characterId}")
    public ApiResponse<Map<String, Object>> updateCharacterMemoryConfig(
            @PathVariable String characterId,
            @RequestBody Map<String, Object> config) {
        Map<String, Object> updated = memoryApplication.updateCharacterMemoryConfig(characterId, config);
        return ApiResponse.success(updated);
    }

    /**
     * 21.3 获取 Workspace 记忆配置
     * GET /api/v1/memory/config/workspace/:workspaceId
     */
    @Operation(summary = "获取 Workspace 记忆配置")
    @GetMapping("/config/workspace/{workspaceId}")
    public ApiResponse<Map<String, Object>> getWorkspaceMemoryConfig(
            @PathVariable String workspaceId) {
        Map<String, Object> config = memoryApplication.getWorkspaceMemoryConfig(workspaceId);
        return ApiResponse.success(config);
    }

    /**
     * 21.4 更新 Workspace 记忆配置
     * PUT /api/v1/memory/config/workspace/:workspaceId
     */
    @Operation(summary = "更新 Workspace 记忆配置")
    @PutMapping("/config/workspace/{workspaceId}")
    public ApiResponse<Map<String, Object>> updateWorkspaceMemoryConfig(
            @PathVariable String workspaceId,
            @RequestBody Map<String, Object> config) {
        Map<String, Object> updated = memoryApplication.updateWorkspaceMemoryConfig(workspaceId, config);
        return ApiResponse.success(updated);
    }

    /**
     * 21.5 获取记忆运行时状态
     * GET /api/v1/memory/runtime-status
     */
    @Operation(summary = "获取记忆运行时状态")
    @GetMapping("/runtime-status")
    public ApiResponse<Map<String, Object>> getRuntimeStatus() {
        Map<String, Object> status = memoryApplication.getRuntimeStatus();
        return ApiResponse.success(status);
    }

    // ==================== 请求体 DTO ====================

    /** 添加数据源请求 */
    @Data
    public static class AddSourceRequest {
        private String title;
        private String sourceType;
        private String sourcePath;
        private String backend;
        private String provider;
    }

    /** 记忆片段请求 */
    @Data
    public static class ChunkRequest {
        private String fileId;
        private String title;
        private String content;
        private String summary;
        private List<String> tags;
    }

    /** 创建绑定请求 */
    @Data
    public static class CreateBindingRequest {
        private String fileId;
        private String targetType;
        private String targetId;
        private String mountType;
        private List<String> selectedChunkIds;
        private List<Map<String, Object>> mountRules;
    }

    /** 检索请求 */
    @Data
    public static class SearchRequest {
        private String query;
        private String scopeType;
        private String scopeId;
        private Integer topK;
        private Double minScore;
    }
}

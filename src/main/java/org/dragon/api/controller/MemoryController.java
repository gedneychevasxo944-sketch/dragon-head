package org.dragon.api.controller;

import org.dragon.api.controller.dto.memory.*;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.memory.core.SourceDocumentService;
import org.dragon.memory.core.MemoryFileService;
import org.dragon.memory.core.MemoryChunkService;
import org.dragon.memory.core.BindingService;
import org.dragon.memory.core.RetrievalService;
import org.dragon.memory.core.OperationsService;
import org.dragon.memory.core.StatsService;
import org.dragon.permission.checker.PermissionChecker;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Memory 模块控制器
 * 提供 Memory 模块的所有 HTTP 接口
 *
 * @author binarytom
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/memory")
public class MemoryController {
    private final PermissionChecker permissionChecker;
    private final SourceDocumentService sourceDocumentService;
    private final MemoryFileService memoryFileService;
    private final MemoryChunkService memoryChunkService;
    private final BindingService bindingService;
    private final RetrievalService retrievalService;
    private final OperationsService operationsService;
    private final StatsService statsService;

    public MemoryController(PermissionChecker permissionChecker,
                           SourceDocumentService sourceDocumentService,
                           MemoryFileService memoryFileService,
                           MemoryChunkService memoryChunkService,
                           BindingService bindingService,
                           RetrievalService retrievalService,
                           OperationsService operationsService,
                           StatsService statsService) {
        this.permissionChecker = permissionChecker;
        this.sourceDocumentService = sourceDocumentService;
        this.memoryFileService = memoryFileService;
        this.memoryChunkService = memoryChunkService;
        this.bindingService = bindingService;
        this.retrievalService = retrievalService;
        this.operationsService = operationsService;
        this.statsService = statsService;
    }

    // ==================== 数据源管理接口 ====================

    /**
     * 获取数据源列表
     */
    @GetMapping("/sources")
    public ApiResponse<PageResponse<SourceDocumentDTO>> getSources(@RequestParam(required = false) String search,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) String sourceType,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(sourceDocumentService.getSources(search, status, sourceType, page, pageSize));
    }

    /**
     * 获取数据源详情
     */
    @GetMapping("/sources/{sourceId}")
    public ApiResponse<SourceDocumentDTO> getSource(@PathVariable String sourceId) {
        return ApiResponse.success(sourceDocumentService.getSource(sourceId));
    }

    /**
     * 创建数据源
     */
    @PostMapping("/sources")
    public ApiResponse<SourceDocumentDTO> createSource(@RequestBody CreateSourceRequest request) {
        return ApiResponse.success(sourceDocumentService.createSource(request));
    }

    /**
     * 更新数据源
     */
    @PutMapping("/sources/{sourceId}")
    public ApiResponse<SourceDocumentDTO> updateSource(@PathVariable String sourceId,
                                                       @RequestBody UpdateSourceRequest request) {
        return ApiResponse.success(sourceDocumentService.updateSource(sourceId, request));
    }

    /**
     * 删除数据源
     */
    @DeleteMapping("/sources/{sourceId}")
    public ApiResponse<Boolean> deleteSource(@PathVariable String sourceId) {
        return ApiResponse.success(sourceDocumentService.deleteSource(sourceId));
    }

    /**
     * 同步数据源
     */
    @PostMapping("/sources/{sourceId}/sync")
    public ApiResponse<SyncResultDTO> syncSource(@PathVariable String sourceId) {
        return ApiResponse.success(SyncResultDTO.builder()
                .success(true)
                .message(sourceDocumentService.syncSource(sourceId))
                .build());
    }

    // ==================== 记忆文件管理接口 ====================

    /**
     * 获取文件列表
     */
    @GetMapping("/files")
    public ApiResponse<PageResponse<MemoryFileDTO>> getFiles(@RequestParam(required = false) String sourceId,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(memoryFileService.getFiles(sourceId, page, pageSize));
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/files/{fileId}")
    public ApiResponse<MemoryFileDTO> getFile(@PathVariable String fileId) {
        return ApiResponse.success(memoryFileService.getFile(fileId));
    }

    /**
     * 获取文件的片段
     */
    @GetMapping("/files/{fileId}/chunks")
    public ApiResponse<PageResponse<MemoryChunkDTO>> getFileChunks(@PathVariable String fileId,
                                                                   @RequestParam(required = false) String indexedStatus,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(memoryChunkService.getChunks(fileId, null, indexedStatus, null, null, page, pageSize));
    }

    /**
     * 同步文件
     */
    @PostMapping("/files/{fileId}/sync")
    public ApiResponse<SyncResultDTO> syncFile(@PathVariable String fileId) {
        return ApiResponse.success(SyncResultDTO.builder()
                .success(true)
                .message(memoryFileService.syncFile(fileId))
                .build());
    }

    // ==================== 记忆片段管理接口 ====================

    /**
     * 获取片段列表
     */
    @GetMapping("/chunks")
    public ApiResponse<PageResponse<MemoryChunkDTO>> getChunks(@RequestParam(required = false) String fileId,
                                                               @RequestParam(required = false) String sourceId,
                                                               @RequestParam(required = false) String indexedStatus,
                                                               @RequestParam(required = false) String tags,
                                                               @RequestParam(required = false) String search,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(memoryChunkService.getChunks(fileId, sourceId, indexedStatus, tags, search, page, pageSize));
    }

    /**
     * 获取片段详情
     */
    @GetMapping("/chunks/{chunkId}")
    public ApiResponse<MemoryChunkDTO> getChunk(@PathVariable String chunkId) {
        return ApiResponse.success(memoryChunkService.getChunk(chunkId));
    }

    /**
     * 创建片段
     */
    @PostMapping("/chunks")
    public ApiResponse<MemoryChunkDTO> createChunk(@RequestBody CreateChunkRequest request) {
        return ApiResponse.success(memoryChunkService.createChunk(request));
    }

    /**
     * 更新片段
     */
    @PutMapping("/chunks/{chunkId}")
    public ApiResponse<MemoryChunkDTO> updateChunk(@PathVariable String chunkId,
                                                   @RequestBody UpdateChunkRequest request) {
        return ApiResponse.success(memoryChunkService.updateChunk(chunkId, request));
    }

    /**
     * 删除片段
     */
    @DeleteMapping("/chunks/{chunkId}")
    public ApiResponse<Boolean> deleteChunk(@PathVariable String chunkId) {
        return ApiResponse.success(memoryChunkService.deleteChunk(chunkId));
    }

    /**
     * 批量删除片段
     */
    @DeleteMapping("/chunks/batch")
    public ApiResponse<BatchOperationResultDTO> batchDeleteChunks(@RequestBody BatchDeleteChunksRequest request) {
        return ApiResponse.success(memoryChunkService.batchDeleteChunks(request));
    }

    /**
     * 批量更新片段索引状态
     */
    @PutMapping("/chunks/batch/index")
    public ApiResponse<BatchOperationResultDTO> batchUpdateIndexStatus(@RequestBody BatchUpdateIndexStatusRequest request) {
        return ApiResponse.success(memoryChunkService.batchUpdateIndexStatus(request));
    }

    // ==================== 绑定关系管理接口 ====================

    /**
     * 获取绑定列表
     */
    @GetMapping("/bindings")
    public ApiResponse<PageResponse<BindingDTO>> getBindings(@RequestParam(required = false) String fileId,
                                                             @RequestParam(required = false) String targetType,
                                                             @RequestParam(required = false) String targetId,
                                                             @RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(bindingService.getBindings(fileId, targetType, targetId, page, pageSize));
    }

    /**
     * 创建绑定
     */
    @PostMapping("/bindings")
    public ApiResponse<BindingDTO> createBinding(@RequestBody CreateBindingRequest request) {
        return ApiResponse.success(bindingService.createBinding(request));
    }

    /**
     * 更新绑定
     */
    @PutMapping("/bindings/{bindingId}")
    public ApiResponse<BindingDTO> updateBinding(@PathVariable String bindingId,
                                                 @RequestBody UpdateBindingRequest request) {
        return ApiResponse.success(bindingService.updateBinding(bindingId, request));
    }

    /**
     * 删除绑定
     */
    @DeleteMapping("/bindings/{bindingId}")
    public ApiResponse<Boolean> deleteBinding(@PathVariable String bindingId) {
        return ApiResponse.success(bindingService.deleteBinding(bindingId));
    }

    // ==================== 检索接口 ====================

    /**
     * 检索记忆内容
     */
    @PostMapping("/retrieval/search")
    public ApiResponse<RetrievalResponseDTO> search(@RequestBody RetrievalRequestDTO request) {
        return ApiResponse.success(retrievalService.search(request));
    }

    /**
     * 检索测试
     */
    @PostMapping("/retrieval/test")
    public ApiResponse<RetrievalResponseDTO> testRetrieval(@RequestBody RetrievalRequestDTO request) {
        return ApiResponse.success(retrievalService.testRetrieval(request));
    }

    // ==================== 运维接口 ====================

    /**
     * 获取运行时状态
     */
    @GetMapping("/operations/status")
    public ApiResponse<RuntimeStatusDTO> getRuntimeStatus(@RequestParam(defaultValue = "all") String scope,
                                                          @RequestParam(required = false) String targetId) {
        return ApiResponse.success(operationsService.getRuntimeStatus(scope, targetId));
    }

    /**
     * 执行系统检查
     */
    @PostMapping("/operations/probe")
    public ApiResponse<ProbeResultDTO> probe(@RequestParam(defaultValue = "all") String scope,
                                             @RequestParam(required = false) String targetId) {
        return ApiResponse.success(operationsService.probe(scope, targetId));
    }

    /**
     * 重建索引
     */
    @PostMapping("/operations/reindex")
    public ApiResponse<SyncResultDTO> reindex(@RequestParam(defaultValue = "all") String scope,
                                              @RequestParam(required = false) String targetId,
                                              @RequestParam(required = false) String sourceId) {
        return ApiResponse.success(operationsService.reindex(scope, targetId, sourceId));
    }

    /**
     * 清除缓存
     */
    @PostMapping("/operations/clear-cache")
    public ApiResponse<BatchOperationResultDTO> clearCache(@RequestParam(defaultValue = "all") String scope,
                                                          @RequestParam(required = false) String targetId) {
        return ApiResponse.success(operationsService.clearCache(scope, targetId));
    }

    // ==================== 统计接口 ====================

    /**
     * 获取全局统计信息
     */
    @GetMapping("/stats/overview")
    public ApiResponse<MemoryOverviewStatsDTO> getOverviewStats() {
        return ApiResponse.success(statsService.getOverviewStats());
    }

    /**
     * 获取数据源统计信息
     */
    @GetMapping("/stats/sources")
    public ApiResponse<SourceStatsResponseDTO> getSourceStats() {
        return ApiResponse.success(statsService.getSourceStats());
    }
}

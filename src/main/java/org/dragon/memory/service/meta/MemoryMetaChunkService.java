package org.dragon.memory.service.meta;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.BatchDeleteChunksRequest;
import org.dragon.api.controller.dto.memory.BatchOperationResultDTO;
import org.dragon.api.controller.dto.memory.BatchUpdateIndexStatusRequest;
import org.dragon.api.controller.dto.memory.CreateChunkRequest;
import org.dragon.api.controller.dto.memory.MemoryChunkDTO;
import org.dragon.api.controller.dto.memory.UpdateChunkRequest;

/**
 * 记忆片段服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryMetaChunkService {

    /**
     * 获取片段列表
     *
     * @param sourceId      数据源 ID（可选）
     * @param syncStatus    同步状态过滤（可选）
     * @param indexedStatus 索引状态过滤（可选）
     * @param tags          标签过滤（可选）
     * @param search        搜索关键词（可选）
     * @param page          页码
     * @param pageSize      每页大小
     * @return 片段分页列表
     */
    PageResponse<MemoryChunkDTO> getChunks(String sourceId, String syncStatus, String indexedStatus, String tags, String search, int page, int pageSize);

    /**
     * 获取片段详情
     *
     * @param chunkId 片段 ID
     * @return 片段详情
     */
    MemoryChunkDTO getChunk(String chunkId);

    /**
     * 创建片段
     *
     * @param request 创建片段请求
     * @return 已创建的片段
     */
    MemoryChunkDTO createChunk(CreateChunkRequest request);

    /**
     * 更新片段
     *
     * @param chunkId 片段 ID
     * @param request 更新片段请求
     * @return 已更新的片段
     */
    MemoryChunkDTO updateChunk(String chunkId, UpdateChunkRequest request);

    /**
     * 删除片段
     *
     * @param chunkId 片段 ID
     * @return 是否成功
     */
    boolean deleteChunk(String chunkId);

    /**
     * 批量删除片段
     *
     * @param request 批量删除请求
     * @return 批量操作结果
     */
    BatchOperationResultDTO batchDeleteChunks(BatchDeleteChunksRequest request);

    /**
     * 批量更新片段索引状态
     *
     * @param request 批量更新索引状态请求
     * @return 批量操作结果
     */
    BatchOperationResultDTO batchUpdateIndexStatus(BatchUpdateIndexStatusRequest request);

    /**
     * 同步片段文件（更新 syncStatus）
     *
     * @param chunkId 片段 ID
     * @return 同步结果描述
     */
    String syncChunk(String chunkId);
}
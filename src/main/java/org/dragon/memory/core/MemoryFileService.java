package org.dragon.memory.core;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.MemoryFileDTO;

/**
 * 记忆文件服务接口
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryFileService {
    /**
     * 获取文件列表
     *
     * @param sourceId  数据源 ID（可选）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 文件分页列表
     */
    PageResponse<MemoryFileDTO> getFiles(String sourceId, int page, int pageSize);

    /**
     * 获取文件详情
     *
     * @param fileId 文件 ID
     * @return 文件详情
     */
    MemoryFileDTO getFile(String fileId);

    /**
     * 同步文件
     *
     * @param fileId 文件 ID
     * @return 同步结果
     */
    String syncFile(String fileId);
}

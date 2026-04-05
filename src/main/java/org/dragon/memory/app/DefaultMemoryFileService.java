package org.dragon.memory.app;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.MemoryFileDTO;
import org.dragon.memory.core.MemoryFileService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 记忆文件服务实现类
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryFileService implements MemoryFileService {
    @Override
    public PageResponse<MemoryFileDTO> getFiles(String sourceId, int page, int pageSize) {
        // TODO: 实现文件列表查询逻辑
        return PageResponse.<MemoryFileDTO>builder()
                .list(List.of())
                .total(0)
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public MemoryFileDTO getFile(String fileId) {
        // TODO: 实现文件详情查询逻辑
        return null;
    }

    @Override
    public String syncFile(String fileId) {
        // TODO: 实现文件同步逻辑
        return "同步成功";
    }
}

package org.dragon.memory.app;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.MemoryFileDTO;
import org.dragon.memory.core.MemoryFileService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;

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
        // Mock 实现：返回一些示例文件数据
        List<MemoryFileDTO> mockFiles = List.of(
                MemoryFileDTO.builder()
                        .id("file-1")
                        .sourceId(sourceId)
                        .title("示例文件 1")
                        .description("这是一个示例文件")
                        .filePath("/path/to/file1.md")
                        .fileType("markdown")
                        .chunkCount(5)
                        .totalSize(1024)
                        .syncStatus("synced")
                        .healthStatus("healthy")
                        .lastSyncAt(Instant.now())
                        .createdAt(Instant.now().minusSeconds(3600))
                        .updatedAt(Instant.now())
                        .build(),
                MemoryFileDTO.builder()
                        .id("file-2")
                        .sourceId(sourceId)
                        .title("示例文件 2")
                        .description("这是另一个示例文件")
                        .filePath("/path/to/file2.txt")
                        .fileType("text")
                        .chunkCount(3)
                        .totalSize(512)
                        .syncStatus("syncing")
                        .healthStatus("warning")
                        .lastSyncAt(Instant.now().minusSeconds(60))
                        .createdAt(Instant.now().minusSeconds(7200))
                        .updatedAt(Instant.now().minusSeconds(30))
                        .build()
        );

        return PageResponse.<MemoryFileDTO>builder()
                .list(mockFiles)
                .total(mockFiles.size())
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

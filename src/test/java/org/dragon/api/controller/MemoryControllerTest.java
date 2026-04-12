package org.dragon.api.controller;

import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.SourceDocumentDTO;
import org.dragon.memory.service.meta.MemoryMetaBindingService;
import org.dragon.memory.service.meta.MemoryMetaChunkService;
import org.dragon.memory.service.meta.MemoryMetaSourceDocumentService;
import org.dragon.memory.service.core.StatsService;
import org.dragon.permission.checker.PermissionChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MemoryController 测试类
 *
 * @author binarytom
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class MemoryControllerTest {

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private MemoryMetaSourceDocumentService sourceDocumentService;

    @Mock
    private MemoryMetaChunkService memoryChunkService;

    @Mock
    private MemoryMetaBindingService bindingService;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private MemoryMetaController memoryMetaController;

    @BeforeEach
    void setUp() {
        // 模拟权限检查（使用 lenient 存根）
        lenient().doNothing().when(permissionChecker).checkEdit(anyString(), anyString());
        lenient().doNothing().when(permissionChecker).checkView(anyString(), anyString());
        lenient().doNothing().when(permissionChecker).checkDelete(anyString(), anyString());
    }

    @Test
    void testGetSources() {
        // 模拟数据源列表
        PageResponse<SourceDocumentDTO> pageResponse = PageResponse.<SourceDocumentDTO>builder()
                .list(Collections.emptyList())
                .total(0)
                .page(1)
                .pageSize(20)
                .build();
        when(sourceDocumentService.getSources(any(), any(), any(), anyInt(), anyInt())).thenReturn(pageResponse);

        // 调用接口
        ApiResponse<PageResponse<SourceDocumentDTO>> response = memoryMetaController.getSources(null, null, null, 1, 20);

        // 验证响应
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertEquals("成功", response.getMessage());
        assertNotNull(response.getData());
        assertTrue(response.getData().getList().isEmpty());
        assertEquals(0, response.getData().getTotal());
        assertEquals(1, response.getData().getPage());
        assertEquals(20, response.getData().getPageSize());

        // 验证方法调用
        verify(sourceDocumentService).getSources(null, null, null, 1, 20);
    }

    @Test
    void testCreateSource() {
        // 模拟创建数据源请求
        SourceDocumentDTO sourceDocumentDTO = SourceDocumentDTO.builder()
                .id("1")
                .title("测试数据源")
                .sourcePath("/test/path")
                .sourceType("file")
                .enabled(true)
                .build();
        when(sourceDocumentService.createSource(any())).thenReturn(sourceDocumentDTO);

        // 调用接口
        ApiResponse<SourceDocumentDTO> response = memoryMetaController.createSource(any());

        // 验证响应
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertEquals("成功", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("1", response.getData().getId());
        assertEquals("测试数据源", response.getData().getTitle());
        assertEquals("/test/path", response.getData().getSourcePath());
        assertEquals("file", response.getData().getSourceType());
        assertTrue(response.getData().isEnabled());

        // 验证方法调用
        verify(sourceDocumentService).createSource(any());
    }

    @Test
    void testDeleteSource() {
        // 模拟删除数据源响应
        when(sourceDocumentService.deleteSource(any())).thenReturn(true);

        // 调用接口
        ApiResponse<Boolean> response = memoryMetaController.deleteSource("1");

        // 验证响应
        assertNotNull(response);
        assertEquals(0, response.getCode());
        assertEquals("成功", response.getMessage());
        assertTrue(response.getData());

        // 验证方法调用
        verify(sourceDocumentService).deleteSource("1");
    }
}

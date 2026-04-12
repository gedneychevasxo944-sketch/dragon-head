package org.dragon.memory;

import org.dragon.api.controller.dto.PageResponse;
import org.dragon.api.controller.dto.memory.BatchDeleteChunksRequest;
import org.dragon.api.controller.dto.memory.BatchOperationResultDTO;
import org.dragon.api.controller.dto.memory.BatchUpdateIndexStatusRequest;
import org.dragon.api.controller.dto.memory.CreateChunkRequest;
import org.dragon.api.controller.dto.memory.MemoryChunkDTO;
import org.dragon.api.controller.dto.memory.UpdateChunkRequest;
import org.dragon.memory.app.DefaultMemoryChunkService;
import org.dragon.memory.store.MemoryChunkStore;
import org.dragon.memory.store.MemoryMemoryChunkStore;
import org.dragon.store.StoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DefaultMemoryChunkService 单元测试
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryChunkServiceTest {

    private MemoryMemoryChunkStore chunkStore;
    private DefaultMemoryChunkService chunkService;

    @BeforeEach
    void setUp() {
        chunkStore = new MemoryMemoryChunkStore();

        StoreFactory storeFactory = mock(StoreFactory.class);
        when(storeFactory.get(MemoryChunkStore.class)).thenReturn(chunkStore);

        chunkService = new DefaultMemoryChunkService(storeFactory);
    }

    // ---- createChunk ----

    @Test
    void testCreateChunkPersistsToStore() {
        CreateChunkRequest request = CreateChunkRequest.builder()
                .sourceId("source-001")
                .title("Test Chunk")
                .content("Some content here")
                .summary("Summary")
                .tags(List.of("tag1", "tag2"))
                .filePath("/data/test.md")
                .fileType("markdown")
                .build();

        MemoryChunkDTO result = chunkService.createChunk(request);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("source-001", result.getSourceId());
        assertEquals("Test Chunk", result.getTitle());
        assertEquals("pending", result.getIndexedStatus());
        assertEquals("pending", result.getSyncStatus());
        assertEquals("unknown", result.getHealthStatus());

        // 验证持久化到 store
        assertNotNull(chunkStore.findById(result.getId()).orElse(null),
                "Chunk should be persisted in store");
    }

    @Test
    void testCreateChunkSerializesTags() {
        CreateChunkRequest request = CreateChunkRequest.builder()
                .sourceId("source-002")
                .title("Tagged Chunk")
                .tags(List.of("alpha", "beta", "gamma"))
                .build();

        MemoryChunkDTO result = chunkService.createChunk(request);

        assertNotNull(result.getTags());
        assertEquals(3, result.getTags().size());
        assertTrue(result.getTags().contains("alpha"));
        assertTrue(result.getTags().contains("beta"));
        assertTrue(result.getTags().contains("gamma"));
    }

    @Test
    void testCreateChunkWithNullTagsReturnsEmptyList() {
        CreateChunkRequest request = CreateChunkRequest.builder()
                .sourceId("source-003")
                .title("No Tags")
                .build();

        MemoryChunkDTO result = chunkService.createChunk(request);

        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void testCreateChunkFileMetadataMapped() {
        CreateChunkRequest request = CreateChunkRequest.builder()
                .sourceId("source-004")
                .title("File Chunk")
                .filePath("/docs/notes.md")
                .fileType("markdown")
                .totalSize(2048L)
                .build();

        MemoryChunkDTO result = chunkService.createChunk(request);

        assertEquals("/docs/notes.md", result.getFilePath());
        assertEquals("markdown", result.getFileType());
        assertEquals(2048L, result.getTotalSize());
    }

    // ---- getChunk ----

    @Test
    void testGetChunkReturnsCorrectDto() {
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("source-get")
                .title("Get Me")
                .content("Content")
                .build());

        MemoryChunkDTO found = chunkService.getChunk(created.getId());

        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
        assertEquals("Get Me", found.getTitle());
    }

    @Test
    void testGetChunkReturnsNullForMissingId() {
        assertNull(chunkService.getChunk("non-existent-id"),
                "getChunk should return null for unknown id");
    }

    // ---- getChunks (paging) ----

    @Test
    void testGetChunksPaged() {
        String sourceId = "source-paged";
        for (int i = 0; i < 5; i++) {
            chunkService.createChunk(CreateChunkRequest.builder()
                    .sourceId(sourceId)
                    .title("Chunk " + i)
                    .build());
        }

        PageResponse<MemoryChunkDTO> page = chunkService.getChunks(sourceId, null, null, null, null, 1, 3);

        assertEquals(5L, page.getTotal());
        assertEquals(3, page.getList().size());
    }

    @Test
    void testGetChunksFilterByIndexedStatus() {
        chunkService.createChunk(CreateChunkRequest.builder().sourceId("s1").title("A").build());
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s1").title("B").build());

        // 手动更新其中一个为 indexed
        chunkService.batchUpdateIndexStatus(BatchUpdateIndexStatusRequest.builder()
                .chunkIds(List.of(created.getId()))
                .indexedStatus("indexed")
                .build());

        PageResponse<MemoryChunkDTO> page = chunkService.getChunks(null, null, "indexed", null, null, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals("indexed", page.getList().get(0).getIndexedStatus());
    }

    @Test
    void testGetChunksFilterBySyncStatus() {
        MemoryChunkDTO c1 = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s1").title("A").build());
        chunkService.createChunk(CreateChunkRequest.builder().sourceId("s1").title("B").build());

        // 同步 c1
        chunkService.syncChunk(c1.getId());

        PageResponse<MemoryChunkDTO> page = chunkService.getChunks(null, "synced", null, null, null, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals(c1.getId(), page.getList().get(0).getId());
    }

    // ---- updateChunk ----

    @Test
    void testUpdateChunkChangesTitleAndContent() {
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("source-upd")
                .title("Old Title")
                .content("Old Content")
                .build());

        MemoryChunkDTO updated = chunkService.updateChunk(created.getId(), UpdateChunkRequest.builder()
                .title("New Title")
                .content("New Content")
                .build());

        assertNotNull(updated);
        assertEquals("New Title", updated.getTitle());
        assertEquals("New Content", updated.getContent());
    }

    @Test
    void testUpdateChunkUpdatesIndexedStatus() {
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("source-status")
                .title("Status Test")
                .build());

        assertEquals("pending", created.getIndexedStatus());

        MemoryChunkDTO updated = chunkService.updateChunk(created.getId(), UpdateChunkRequest.builder()
                .indexedStatus("indexed")
                .build());

        assertNotNull(updated);
        assertEquals("indexed", updated.getIndexedStatus());
    }

    @Test
    void testUpdateChunkFileMetadata() {
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("source-file-upd")
                .title("File Update Test")
                .build());

        MemoryChunkDTO updated = chunkService.updateChunk(created.getId(), UpdateChunkRequest.builder()
                .filePath("/new/path.md")
                .fileType("markdown")
                .syncStatus("synced")
                .healthStatus("healthy")
                .build());

        assertNotNull(updated);
        assertEquals("/new/path.md", updated.getFilePath());
        assertEquals("markdown", updated.getFileType());
        assertEquals("synced", updated.getSyncStatus());
        assertEquals("healthy", updated.getHealthStatus());
    }

    @Test
    void testUpdateChunkReturnsNullForMissingId() {
        MemoryChunkDTO result = chunkService.updateChunk("missing-id", UpdateChunkRequest.builder()
                .title("Whatever")
                .build());

        assertNull(result, "updateChunk should return null for unknown id");
    }

    // ---- deleteChunk ----

    @Test
    void testDeleteChunkRemovesFromStore() {
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("source-del")
                .title("Delete Me")
                .build());

        assertTrue(chunkService.deleteChunk(created.getId()));
        assertNull(chunkService.getChunk(created.getId()),
                "Chunk should be gone after delete");
    }

    @Test
    void testDeleteNonExistentChunkReturnsFalse() {
        assertFalse(chunkService.deleteChunk("non-existent"),
                "deleteChunk should return false for unknown id");
    }

    // ---- batchDeleteChunks ----

    @Test
    void testBatchDeleteChunks() {
        MemoryChunkDTO c1 = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s").title("C1").build());
        MemoryChunkDTO c2 = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s").title("C2").build());
        MemoryChunkDTO c3 = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s").title("C3").build());

        BatchOperationResultDTO result = chunkService.batchDeleteChunks(
                BatchDeleteChunksRequest.builder()
                        .chunkIds(List.of(c1.getId(), c2.getId()))
                        .build());

        assertTrue(result.isSuccess());
        assertEquals(2, result.getDeletedCount());
        assertNull(chunkService.getChunk(c1.getId()));
        assertNull(chunkService.getChunk(c2.getId()));
        assertNotNull(chunkService.getChunk(c3.getId()), "c3 should still exist");
    }

    // ---- batchUpdateIndexStatus ----

    @Test
    void testBatchUpdateIndexStatus() {
        MemoryChunkDTO c1 = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s").title("C1").build());
        MemoryChunkDTO c2 = chunkService.createChunk(CreateChunkRequest.builder().sourceId("s").title("C2").build());

        BatchOperationResultDTO result = chunkService.batchUpdateIndexStatus(
                BatchUpdateIndexStatusRequest.builder()
                        .chunkIds(List.of(c1.getId(), c2.getId()))
                        .indexedStatus("indexed")
                        .build());

        assertTrue(result.isSuccess());
        assertEquals(2, result.getUpdatedCount());
        assertEquals("indexed", chunkService.getChunk(c1.getId()).getIndexedStatus());
        assertEquals("indexed", chunkService.getChunk(c2.getId()).getIndexedStatus());
    }

    // ---- syncChunk ----

    @Test
    void testSyncChunkUpdatesSyncStatusAndLastSyncAt() {
        MemoryChunkDTO created = chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("source-sync")
                .title("Sync Me")
                .build());

        assertEquals("pending", created.getSyncStatus());

        String result = chunkService.syncChunk(created.getId());

        assertEquals("同步成功", result);
        MemoryChunkDTO synced = chunkService.getChunk(created.getId());
        assertEquals("synced", synced.getSyncStatus());
        assertNotNull(synced.getLastSyncAt(), "lastSyncAt should be set after sync");
    }

    @Test
    void testSyncChunkReturnsErrorForMissingId() {
        String result = chunkService.syncChunk("non-existent-id");
        assertEquals("片段不存在", result);
    }

    // ---- MemoryMemoryChunkStore 单元测试 ----

    @Test
    void testStoreSearchByContent() {
        chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("s1")
                .title("Dragon Memory")
                .content("This is about dragons")
                .build());
        chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("s1")
                .title("Other Topic")
                .content("Nothing related")
                .build());

        PageResponse<MemoryChunkDTO> page = chunkService.getChunks(null, null, null, null, "dragon", 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals("Dragon Memory", page.getList().get(0).getTitle());
    }

    @Test
    void testStoreSearchByTags() {
        chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("s1")
                .title("Tagged")
                .tags(List.of("important", "review"))
                .build());
        chunkService.createChunk(CreateChunkRequest.builder()
                .sourceId("s1")
                .title("Untagged")
                .build());

        PageResponse<MemoryChunkDTO> page = chunkService.getChunks(null, null, null, "important", null, 1, 10);

        assertEquals(1L, page.getTotal());
        assertEquals("Tagged", page.getList().get(0).getTitle());
    }
}
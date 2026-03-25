package org.dragon.memory;

import org.dragon.memory.builtin.*;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.models.MemorySearchResult;
import org.dragon.memory.models.SearchOptions;
import org.dragon.memory.provider.EmbeddingProvider;
import org.dragon.memory.provider.EmbeddingProviderFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryModuleBasicTest {

    @Test
    public void testEmbeddingProviderCreation() {
        // 测试 EmbeddingProvider 工厂创建
        EmbeddingProvider provider = EmbeddingProviderFactory.create("dummy", "dummy");
        assertNotNull(provider);
        assertEquals("dummy", provider.getProviderKey());
        assertNotNull(provider.getEmbedding("test"));
        assertEquals(10, provider.getDimensions());
    }

    @Test
    public void testMemorySearchConfig() {
        ResolvedMemorySearchConfig config = createTestConfig();
        assertNotNull(config);
        assertTrue(config.isFtsEnabled());
        assertTrue(config.isStoreVectorEnabled());
        assertEquals(512, config.getChunkingTokens());
        assertEquals(50, config.getChunkingOverlap());
    }

    @Test
    public void testMemorySchemaManager() {
        ResolvedMemorySearchConfig config = createTestConfig();
        MemorySchemaManager schemaManager = new MemorySchemaManager(config);
        assertNotNull(schemaManager);
    }

    @Test
    public void testMemoryIndexRepository() {
        ResolvedMemorySearchConfig config = createTestConfig();
        MemoryIndexRepository repository = new MemoryIndexRepository(config);
        assertNotNull(repository);
    }

    @Test
    public void testMemoryFileScanner() {
        ResolvedMemorySearchConfig config = createTestConfig();
        MemoryFileScanner fileScanner = new MemoryFileScanner(config);
        assertNotNull(fileScanner);
        // 测试扫描功能（在实际环境中可能需要调整）
        List<String> files = fileScanner.scanMemoryFiles();
        assertNotNull(files);
    }

    @Test
    public void testMemoryEmbeddingIndexer() {
        ResolvedMemorySearchConfig config = createTestConfig();
        MemoryEmbeddingIndexer indexer = new MemoryEmbeddingIndexer(config);
        assertNotNull(indexer);

        // 测试分块功能
        String testContent = "This is a test document for embedding. It should be split into chunks.";
        List<MemoryEmbeddingIndexer.Chunk> chunks = indexer.chunkMarkdown(testContent);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());

        // 测试向量化功能
        for (MemoryEmbeddingIndexer.Chunk chunk : chunks) {
            List<Double> embedding = indexer.embedQuery(chunk.getContent());
            assertNotNull(embedding);
            assertEquals(10, embedding.size()); // 与 dummy 提供者维度一致
        }
    }

    @Test
    public void testMemoryQueryEngine() {
        ResolvedMemorySearchConfig config = createTestConfig();
        MemoryQueryEngine queryEngine = new MemoryQueryEngine(config);
        assertNotNull(queryEngine);

        SearchOptions options = new SearchOptions();
        options.setMaxResults(10);

        // 测试搜索方法
        List<MemorySearchResult> results = queryEngine.search("test", options);
        assertNotNull(results);
    }

    @Test
    public void testHybridResultMerger() {
        ResolvedMemorySearchConfig config = createTestConfig();
        HybridResultMerger merger = new HybridResultMerger(config);
        assertNotNull(merger);

        // 创建测试结果列表（模拟）
        List<MemorySearchResult> keywordResults = List.of(
                createMockResult("path1.md", "test content", 0.9),
                createMockResult("path2.md", "another test", 0.7)
        );

        List<MemorySearchResult> vectorResults = List.of(
                createMockResult("path2.md", "another test", 0.8),
                createMockResult("path3.md", "related content", 0.6)
        );

        // 测试合并功能
        List<MemorySearchResult> mergedResults = merger.merge(keywordResults, vectorResults);
        assertNotNull(mergedResults);
        assertFalse(mergedResults.isEmpty());
        assertEquals(3, mergedResults.size()); // 去重后的结果数
    }

    @Test
    public void testSyncCoordinator() {
        ResolvedMemorySearchConfig config = createTestConfig();
        MemorySyncCoordinator syncCoordinator = new MemorySyncCoordinator(config);
        assertNotNull(syncCoordinator);
    }

    private ResolvedMemorySearchConfig createTestConfig() {
        ResolvedMemorySearchConfig config = new ResolvedMemorySearchConfig();
        config.setProvider("dummy");
        config.setModel("dummy");
        config.setStorePath("test_memory.db");
        config.setFtsEnabled(true);
        config.setStoreVectorEnabled(true);
        config.setChunkingTokens(512);
        config.setChunkingOverlap(50);
        config.setQueryMaxResults(10);
        config.setQueryMinScore(0.5);
        config.setSyncOnSearchEnabled(true);
        config.setOnSessionStartEnabled(true);
        return config;
    }

    private MemorySearchResult createMockResult(String path, String snippet, double score) {
        MemorySearchResult result = new MemorySearchResult();
        result.setPath(path);
        result.setSnippet(snippet);
        result.setScore(score);
        result.setStartLine(0);
        result.setEndLine(10);
        return result;
    }
}

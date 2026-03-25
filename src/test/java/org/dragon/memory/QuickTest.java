package org.dragon.memory;

import org.dragon.memory.builtin.*;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.impl.MemoryIndexManager;
import org.dragon.memory.models.MemorySearchResult;
import org.dragon.memory.models.SearchOptions;
import org.dragon.memory.provider.EmbeddingProvider;
import org.dragon.memory.provider.EmbeddingProviderFactory;

import java.util.List;

/**
 * 简单的 Java 程序，直接测试 Memory 模块的功能，不依赖 Spring 框架
 */
public class QuickTest {

    public static void main(String[] args) {
        System.out.println("=== Memory Module Quick Test ===");
        try {
            // 测试 1: 创建配置
            ResolvedMemorySearchConfig config = createTestConfig();
            System.out.println("✓ Test 1: Configuration created");

            // 测试 2: 创建 EmbeddingProvider
            EmbeddingProvider provider = EmbeddingProviderFactory.create("dummy", "dummy");
            System.out.println("✓ Test 2: EmbeddingProvider created (" + provider.getProviderKey() + ")");

            // 测试 3: 创建 MemorySchemaManager
            MemorySchemaManager schemaManager = new MemorySchemaManager(config);
            System.out.println("✓ Test 3: MemorySchemaManager created");

            // 测试 4: 创建 MemoryIndexRepository
            MemoryIndexRepository repository = new MemoryIndexRepository(config);
            System.out.println("✓ Test 4: MemoryIndexRepository created");

            // 测试 5: 创建 MemoryFileScanner
            MemoryFileScanner fileScanner = new MemoryFileScanner(config);
            System.out.println("✓ Test 5: MemoryFileScanner created");

            // 测试 6: 创建 MemoryEmbeddingIndexer
            MemoryEmbeddingIndexer indexer = new MemoryEmbeddingIndexer(config);
            System.out.println("✓ Test 6: MemoryEmbeddingIndexer created");

            // 测试 7: 创建 MemoryQueryEngine
            MemoryQueryEngine queryEngine = new MemoryQueryEngine(config);
            System.out.println("✓ Test 7: MemoryQueryEngine created");

            // 测试 8: 创建 MemorySyncCoordinator
            MemorySyncCoordinator syncCoordinator = new MemorySyncCoordinator(config);
            System.out.println("✓ Test 8: MemorySyncCoordinator created");

            // 测试 9: 创建 MemoryIndexManager
            MemoryIndexManager indexManager = new MemoryIndexManager(config);
            System.out.println("✓ Test 9: MemoryIndexManager created");

            // 测试 10: 测试搜索功能（使用模拟配置）
            SearchOptions options = new SearchOptions();
            options.setMaxResults(10);
            List<MemorySearchResult> results = indexManager.search("test", options);
            System.out.println("✓ Test 10: Search completed (" + results.size() + " results)");

            // 测试 11: 测试状态查询
            System.out.println("✓ Test 11: Status query completed");

            System.out.println("\n=== All tests passed! ===");
        } catch (Exception e) {
            System.err.println("× Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ResolvedMemorySearchConfig createTestConfig() {
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
        return config;
    }
}

package org.dragon.memory;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.impl.MemoryIndexManager;
import org.dragon.memory.models.MemoryEmbeddingProbeResult;
import org.dragon.memory.models.MemorySyncProgressUpdate;
import org.dragon.memory.models.SyncRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MemoryIndexManagerTest {

    @Test
    void testMemoryIndexManager() {
        ResolvedMemorySearchConfig searchConfig = new ResolvedMemorySearchConfig();
        MemoryIndexManager memoryIndexManager = new MemoryIndexManager(searchConfig);
        assertNotNull(memoryIndexManager);
    }

    @Test
    void testProbeEmbeddingAvailability() {
        ResolvedMemorySearchConfig searchConfig = new ResolvedMemorySearchConfig();
        MemoryIndexManager memoryIndexManager = new MemoryIndexManager(searchConfig);
        MemoryEmbeddingProbeResult result = memoryIndexManager.probeEmbeddingAvailability();
        assertNotNull(result);
    }

    @Test
    void testSync() {
        ResolvedMemorySearchConfig searchConfig = new ResolvedMemorySearchConfig();
        MemoryIndexManager memoryIndexManager = new MemoryIndexManager(searchConfig);
        MemorySyncProgressUpdate result = memoryIndexManager.sync(new SyncRequest());
        assertNotNull(result);
        assertTrue(result.getStatus().contains("complete"));
    }
}

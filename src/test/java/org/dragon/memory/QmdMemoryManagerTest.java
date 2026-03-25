package org.dragon.memory;

import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.impl.QmdMemoryManager;
import org.dragon.memory.models.MemoryEmbeddingProbeResult;
import org.dragon.memory.models.MemorySyncProgressUpdate;
import org.dragon.memory.models.SyncRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class QmdMemoryManagerTest {

    @Test
    void testQmdMemoryManager() {
        ResolvedMemorySearchConfig searchConfig = new ResolvedMemorySearchConfig();
        QmdMemoryManager qmdMemoryManager = new QmdMemoryManager(searchConfig);
        assertNotNull(qmdMemoryManager);
    }

    @Test
    void testProbeEmbeddingAvailability() {
        ResolvedMemorySearchConfig searchConfig = new ResolvedMemorySearchConfig();
        QmdMemoryManager qmdMemoryManager = new QmdMemoryManager(searchConfig);
        MemoryEmbeddingProbeResult result = qmdMemoryManager.probeEmbeddingAvailability();
        assertNotNull(result);
    }

    @Test
    void testSync() {
        ResolvedMemorySearchConfig searchConfig = new ResolvedMemorySearchConfig();
        QmdMemoryManager qmdMemoryManager = new QmdMemoryManager(searchConfig);
        MemorySyncProgressUpdate result = qmdMemoryManager.sync(new SyncRequest());
        assertNotNull(result);
        assertTrue(result.getStatus().contains("complete"));
    }
}

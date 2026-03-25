package org.dragon.memory;

import org.dragon.memory.config.ResolvedMemoryBackendConfig;
import org.dragon.memory.config.ResolvedMemorySearchConfig;
import org.dragon.memory.impl.MemoryIndexManager;
import org.dragon.memory.impl.QmdMemoryManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.main.allow-circular-references=true")
class MemorySearchManagerFactoryTest {

    @Autowired
    private MemorySearchManagerFactory memorySearchManagerFactory;

    @Test
    void testCreateMemorySearchManager() {
        Map<String, Object> config = new HashMap<>();
        config.put("backend", "builtin");

        MemorySearchManager manager = memorySearchManagerFactory.getMemorySearchManager(config);
        assertNotNull(manager);
        assertTrue(manager instanceof MemoryIndexManager);
    }

    @Test
    void testCreateQmdMemoryManager() {
        Map<String, Object> config = new HashMap<>();
        config.put("backend", "qmd");

        MemorySearchManager manager = memorySearchManagerFactory.getMemorySearchManager(config);
        assertNotNull(manager);
        assertTrue(manager instanceof QmdMemoryManager);
    }
}

package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

public class AtomicReindexService {

    private final ResolvedMemorySearchConfig searchConfig;

    public AtomicReindexService(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    public void performAtomicReindex() {
        // TODO: 实现原子性重建索引的功能
    }
}

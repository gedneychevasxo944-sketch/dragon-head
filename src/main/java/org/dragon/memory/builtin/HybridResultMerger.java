package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

public class HybridResultMerger {

    private final ResolvedMemorySearchConfig searchConfig;

    public HybridResultMerger(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    // TODO: 实现权重融合、MMR、时间衰减等混合排序功能
}

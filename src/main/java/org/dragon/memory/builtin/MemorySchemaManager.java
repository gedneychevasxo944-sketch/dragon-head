package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

public class MemorySchemaManager {

    private final ResolvedMemorySearchConfig searchConfig;

    public MemorySchemaManager(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    public void createTables() {
        // TODO: 创建 SQLite 表和索引
    }

    public void upgradeSchema() {
        // TODO: 实现 schema 演进
    }

    public void createIndices() {
        // TODO: 创建 FTS 和向量索引
    }
}

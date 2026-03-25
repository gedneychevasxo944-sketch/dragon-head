package org.dragon.memory.builtin;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

public class MemoryIndexRepository {

    private final ResolvedMemorySearchConfig searchConfig;

    public MemoryIndexRepository(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
    }

    // TODO: 实现 SQLite 数据库访问方法，包括查询、插入、更新、删除等操作
}

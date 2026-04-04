package org.dragon.memory.core.query;

import org.dragon.memory.core.MemoryQuery;

/**
 * 记忆召回查询
 *
 * @author binarytom
 * @version 1.0
 */
public class RecallQuery {
    private final MemoryQuery memoryQuery;

    private RecallQuery(Builder builder) {
        this.memoryQuery = builder.memoryQuery;
    }

    public MemoryQuery getMemoryQuery() {
        return memoryQuery;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MemoryQuery memoryQuery;

        public Builder memoryQuery(MemoryQuery memoryQuery) {
            this.memoryQuery = memoryQuery;
            return this;
        }

        public RecallQuery build() {
            return new RecallQuery(this);
        }
    }
}

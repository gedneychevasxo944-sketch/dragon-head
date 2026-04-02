package org.dragon.memv2.web.dto;

import org.dragon.memv2.core.MemoryQuery;

/**
 * 回忆记忆请求DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class RecallRequest {
    private MemoryQuery query;

    public MemoryQuery getQuery() {
        return query;
    }

    public void setQuery(MemoryQuery query) {
        this.query = query;
    }
}

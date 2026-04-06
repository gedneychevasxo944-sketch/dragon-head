package org.dragon.api.controller.dto.memory;

/**
 * 回忆记忆请求DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class RecallRequest {
    private MemoryQueryDTO query;

    public MemoryQueryDTO getQuery() {
        return query;
    }

    public void setQuery(MemoryQueryDTO query) {
        this.query = query;
    }
}

package org.dragon.api.controller.dto;

import java.util.List;

/**
 * 回忆记忆响应DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class RecallResponse {
    private List<MemorySearchResultDTO> results;

    public List<MemorySearchResultDTO> getResults() {
        return results;
    }

    public void setResults(List<MemorySearchResultDTO> results) {
        this.results = results;
    }
}

package org.dragon.memv2.web.dto;

import org.dragon.memv2.core.MemorySearchResult;

import java.util.List;

/**
 * 回忆记忆响应DTO
 *
 * @author binarytom
 * @version 1.0
 */
public class RecallResponse {
    private List<MemorySearchResult> results;

    public List<MemorySearchResult> getResults() {
        return results;
    }

    public void setResults(List<MemorySearchResult> results) {
        this.results = results;
    }
}

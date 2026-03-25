package org.dragon.memory.qmd;

import org.dragon.memory.config.ResolvedMemorySearchConfig;

import java.util.ArrayList;
import java.util.List;

public class QmdCollectionRegistry {

    private final ResolvedMemorySearchConfig searchConfig;
    private List<String> managedCollections;

    public QmdCollectionRegistry(ResolvedMemorySearchConfig searchConfig) {
        this.searchConfig = searchConfig;
        this.managedCollections = new ArrayList<>();
        // TODO: 初始化管理的 collections
    }

    public List<String> getManagedCollections() {
        return managedCollections;
    }

    public void refresh() {
        // TODO: 刷新 collections 列表
    }
}
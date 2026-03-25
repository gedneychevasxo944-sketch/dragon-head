package org.dragon.memory.builtin;

import java.util.List;

public class MemoryIndexMeta {

    private String model;
    private String provider;
    private String providerKey;
    private List<String> sources;
    private String scopeHash;
    private int chunkTokens;
    private int chunkOverlap;
    private int vectorDims;

    public MemoryIndexMeta() {
    }

    // Getters and Setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

    public String getScopeHash() {
        return scopeHash;
    }

    public void setScopeHash(String scopeHash) {
        this.scopeHash = scopeHash;
    }

    public int getChunkTokens() {
        return chunkTokens;
    }

    public void setChunkTokens(int chunkTokens) {
        this.chunkTokens = chunkTokens;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getVectorDims() {
        return vectorDims;
    }

    public void setVectorDims(int vectorDims) {
        this.vectorDims = vectorDims;
    }
}

package org.dragon.memory.models;

public class MemoryEmbeddingProbeResult {

    private boolean available;
    private String errorMessage;

    public MemoryEmbeddingProbeResult() {
    }

    public MemoryEmbeddingProbeResult(boolean available, String errorMessage) {
        this.available = available;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
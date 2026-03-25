package org.dragon.memory.models;

public class SyncRequest {

    private boolean force;
    private String reason;
    private boolean incremental;

    public SyncRequest() {
    }

    public SyncRequest(boolean force, String reason, boolean incremental) {
        this.force = force;
        this.reason = reason;
        this.incremental = incremental;
    }

    // Getters and Setters
    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }
}
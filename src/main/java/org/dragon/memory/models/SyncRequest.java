package org.dragon.memory.models;

public class SyncRequest {

    private boolean force;
    private String reason;
    private boolean incremental;
    private boolean targetedSessionSync;
    private String sessionKey;

    public SyncRequest() {
    }

    public SyncRequest(boolean force, String reason, boolean incremental) {
        this.force = force;
        this.reason = reason;
        this.incremental = incremental;
    }

    // 便捷方法
    public static SyncRequest forceSync(String reason) {
        SyncRequest request = new SyncRequest();
        request.setForce(true);
        request.setReason(reason);
        request.setIncremental(false);
        return request;
    }

    public static SyncRequest incrementalSync(String reason) {
        SyncRequest request = new SyncRequest();
        request.setForce(false);
        request.setReason(reason);
        request.setIncremental(true);
        return request;
    }

    public static SyncRequest targetedSessionSync(String sessionKey) {
        SyncRequest request = new SyncRequest();
        request.setTargetedSessionSync(true);
        request.setSessionKey(sessionKey);
        request.setIncremental(false);
        return request;
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

    public boolean isTargetedSessionSync() {
        return targetedSessionSync;
    }

    public void setTargetedSessionSync(boolean targetedSessionSync) {
        this.targetedSessionSync = targetedSessionSync;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public SyncRequest withReason(String reason) {
        this.reason = reason;
        return this;
    }

    public SyncRequest withForce(boolean force) {
        this.force = force;
        return this;
    }
}
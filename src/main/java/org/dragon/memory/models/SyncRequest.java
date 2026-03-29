package org.dragon.memory.models;

public class SyncRequest {

    private Boolean force;
    private String reason;
    private Boolean incremental;
    private Boolean targetedSessionSync;
    private String sessionKey;

    public SyncRequest() {
    }

    public SyncRequest(Boolean force, String reason, Boolean incremental) {
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
    public Boolean isForce() {
        return force;
    }

    public void setForce(Boolean force) {
        this.force = force;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(Boolean incremental) {
        this.incremental = incremental;
    }

    public Boolean isTargetedSessionSync() {
        return targetedSessionSync;
    }

    public void setTargetedSessionSync(Boolean targetedSessionSync) {
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

    public SyncRequest withForce(Boolean force) {
        this.force = force;
        return this;
    }
}
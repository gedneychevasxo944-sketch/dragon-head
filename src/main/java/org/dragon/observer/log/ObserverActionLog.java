package org.dragon.observer.log;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.dragon.actionlog.ActionType;

/**
 * Observer 动作日志实体
 * 通用的事件日志记录，支持多种动作类型和目标类型
 *
 * @author wyj
 * @version 1.0
 */
public class ObserverActionLog {

    private String id;
    private String targetType;
    private String targetId;
    private ActionType actionType;
    private String operator;
    private Map<String, Object> details;
    private LocalDateTime createdAt;

    public ObserverActionLog() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    public ObserverActionLog(String targetType, String targetId, ActionType actionType,
                             String operator, Map<String, Object> details) {
        this();
        this.targetType = targetType;
        this.targetId = targetId;
        this.actionType = actionType;
        this.operator = operator;
        this.details = details;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ObserverActionLog{" +
                "id='" + id + '\'' +
                ", targetType='" + targetType + '\'' +
                ", targetId='" + targetId + '\'' +
                ", actionType=" + actionType +
                ", operator='" + operator + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

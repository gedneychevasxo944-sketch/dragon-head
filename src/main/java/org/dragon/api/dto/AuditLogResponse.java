package org.dragon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.observer.actionlog.ActionType;
import org.dragon.observer.actionlog.ObserverActionLog;

import java.time.LocalDateTime;

/**
 * AuditLogResponse 审计日志响应DTO
 * 对应前端 AuditLog 接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private String id;
    private String targetType;
    private String targetId;
    private String targetName;
    private String actionType;
    private String operator;
    private String createdAt;
    private String detailsSummary;

    /**
     * 从 ObserverActionLog 转换
     */
    public static AuditLogResponse from(ObserverActionLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .targetType(mapTargetType(log.getTargetType()))
                .targetId(log.getTargetId())
                .actionType(mapActionType(log.getActionType()))
                .operator(log.getOperator() != null ? log.getOperator() : "")
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "")
                .detailsSummary(extractSummary(log))
                .build();
    }

    /**
     * 映射后端 targetType 到前端 targetType
     */
    private static String mapTargetType(String targetType) {
        if (targetType == null) return "";
        return switch (targetType.toUpperCase()) {
            case "WORKSPACE" -> "workspace";
            case "MEMBER", "CHARACTER" -> "member";
            case "SKILL" -> "skill";
            case "OBSERVER" -> "observer";
            case "TASK", "PLAN" -> "task";
            default -> targetType.toLowerCase();
        };
    }

    /**
     * 映射后端 ActionType 到前端 actionType
     */
    private static String mapActionType(ActionType actionType) {
        if (actionType == null) return "";
        return switch (actionType) {
            // Workspace lifecycle
            case WORKSPACE_CREATE, HIRE, TASK_CREATE, PLAN_GENERATE, SCHEDULE_REGISTER -> "create";
            case WORKSPACE_DELETE, FIRE, TASK_CANCEL -> "delete";
            case WORKSPACE_ACTIVATE, WORKSPACE_DEACTIVATE, WORKSPACE_ARCHIVE,
                 UPDATE_DUTY, AUTO_ASSIGN, TASK_COMPLETE, TASK_FAIL,
                 PLAN_REJECT, PLAN_ROLLBACK -> "update";
            // Bind/Unbind
            case TASK_START, TASK_DECOMPOSE, MEMBER_SELECT, SUBTASK_EXECUTE,
                 LLM_CALL_START, LLM_CALL_COMPLETE, LLM_CALL_FAIL,
                 CHARACTER_RUN_START, CHARACTER_RUN_COMPLETE, CHARACTER_RUN_FAIL,
                 EVALUATION_TRIGGER, OPTIMIZATION_TRIGGER, OPTIMIZATION_EXECUTE,
                 PLAN_REVIEW, PLAN_EXECUTE, SCHEDULE_TRIGGER, SCHEDULE_EXECUTE -> "execute";
            case PLAN_APPROVE -> "approve";
            // Default for any unhandled cases
            default -> {
                String name = actionType.name().toLowerCase();
                yield name;
            }
        };
    }

    /**
     * 从 details 中提取摘要信息
     */
    private static String extractSummary(ObserverActionLog log) {
        if (log.getDetails() == null || log.getDetails().isEmpty()) {
            return "";
        }
        var details = log.getDetails();
        // 尝试提取常见的摘要字段
        if (details.containsKey("message")) {
            return String.valueOf(details.get("message"));
        }
        if (details.containsKey("summary")) {
            return String.valueOf(details.get("summary"));
        }
        if (details.containsKey("description")) {
            return String.valueOf(details.get("description"));
        }
        if (details.containsKey("name")) {
            return "Name: " + details.get("name");
        }
        if (details.containsKey("action")) {
            return String.valueOf(details.get("action"));
        }
        // 如果没有特定字段，返回空字符串而不是整个对象的 toString
        return "";
    }
}

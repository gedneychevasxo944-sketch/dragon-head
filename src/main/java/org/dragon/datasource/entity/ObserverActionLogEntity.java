package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.dragon.actionlog.ActionType;
import org.dragon.observer.log.ObserverActionLog;

/**
 * ObserverActionLogEntity 观察者动作日志实体
 * 映射数据库 observer_action_log 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "observer_action_log")
public class ObserverActionLogEntity {

    @Id
    private String id;

    private String targetType;

    private String targetId;

    private String actionType;

    private String operator;

    @DbJson
    private Map<String, Object> details;

    private LocalDateTime createdAt;

    /**
     * 转换为ObserverActionLog
     */
    public ObserverActionLog toObserverActionLog() {
        ObserverActionLog log = new ObserverActionLog();
        log.setId(this.id);
        log.setTargetType(this.targetType);
        log.setTargetId(this.targetId);
        log.setActionType(this.actionType != null ? ActionType.valueOf(this.actionType) : null);
        log.setOperator(this.operator);
        log.setDetails(this.details);
        log.setCreatedAt(this.createdAt);
        return log;
    }

    /**
     * 从ObserverActionLog创建Entity
     */
    public static ObserverActionLogEntity fromObserverActionLog(ObserverActionLog log) {
        return ObserverActionLogEntity.builder()
                .id(log.getId())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .actionType(log.getActionType() != null ? log.getActionType().name() : null)
                .operator(log.getOperator())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

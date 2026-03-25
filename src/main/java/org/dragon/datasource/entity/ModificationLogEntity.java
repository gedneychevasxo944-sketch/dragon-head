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

import org.dragon.observer.log.ModificationLog;
import java.util.Map;

/**
 * ModificationLogEntity 修改日志实体
 * 映射数据库 modification_log 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "modification_log")
public class ModificationLogEntity {

    @Id
    private String id;

    private String targetType;

    private String targetId;

    private String beforeSnapshot;

    private String afterSnapshot;

    private String triggerSource;

    private String evaluationId;

    private String reason;

    private String operator;

    private LocalDateTime timestamp;

    @DbJson
    private Map<String, Object> extensions;

    /**
     * 转换为ModificationLog
     */
    public ModificationLog toModificationLog() {
        return ModificationLog.builder()
                .id(this.id)
                .targetType(this.targetType != null ? ModificationLog.TargetType.valueOf(this.targetType) : null)
                .targetId(this.targetId)
                .beforeSnapshot(this.beforeSnapshot)
                .afterSnapshot(this.afterSnapshot)
                .triggerSource(this.triggerSource != null ? ModificationLog.TriggerSource.valueOf(this.triggerSource) : null)
                .evaluationId(this.evaluationId)
                .reason(this.reason)
                .operator(this.operator)
                .timestamp(this.timestamp)
                .extensions(this.extensions)
                .build();
    }

    /**
     * 从ModificationLog创建Entity
     */
    public static ModificationLogEntity fromModificationLog(ModificationLog log) {
        return ModificationLogEntity.builder()
                .id(log.getId())
                .targetType(log.getTargetType() != null ? log.getTargetType().name() : null)
                .targetId(log.getTargetId())
                .beforeSnapshot(log.getBeforeSnapshot())
                .afterSnapshot(log.getAfterSnapshot())
                .triggerSource(log.getTriggerSource() != null ? log.getTriggerSource().name() : null)
                .evaluationId(log.getEvaluationId())
                .reason(log.getReason())
                .operator(log.getOperator())
                .timestamp(log.getTimestamp())
                .extensions(log.getExtensions())
                .build();
    }
}

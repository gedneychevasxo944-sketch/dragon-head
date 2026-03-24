package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.schedule.entity.CronType;
import org.dragon.schedule.entity.JobType;
import org.dragon.schedule.entity.MisfirePolicy;

/**
 * CronDefinitionEntity Cron任务定义实体
 * 映射数据库 cron_definition 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cron_definition")
public class CronDefinitionEntity {

    @Id
    private String id;

    private String name;

    private String description;

    private String createdBy;

    private String cronType;

    private String cronExpression;

    private String timezone;

    private Long startTime;

    private Long endTime;

    private Integer maxConcurrent;

    private String jobType;

    private String jobHandler;

    @DbJson
    private Map<String, Object> jobData;

    private String misfirePolicy;

    private Integer timeoutMs;

    private Integer retryCount;

    private Integer retryIntervalMs;

    private String status;

    private Long createdAt;

    private Long updatedAt;

    private Integer version;

    /**
     * 转换为CronDefinition
     */
    public CronDefinition toCronDefinition() {
        return CronDefinition.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .createdBy(this.createdBy)
                .cronType(this.cronType != null ? CronType.valueOf(this.cronType) : null)
                .cronExpression(this.cronExpression)
                .timezone(this.timezone)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .maxConcurrent(this.maxConcurrent)
                .jobType(this.jobType != null ? JobType.valueOf(this.jobType) : null)
                .jobHandler(this.jobHandler)
                .jobData(this.jobData)
                .misfirePolicy(this.misfirePolicy != null ? MisfirePolicy.valueOf(this.misfirePolicy) : null)
                .timeoutMs(this.timeoutMs)
                .retryCount(this.retryCount)
                .retryIntervalMs(this.retryIntervalMs)
                .status(this.status != null ? CronStatus.valueOf(this.status) : null)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .version(this.version)
                .build();
    }

    /**
     * 从CronDefinition创建Entity
     */
    public static CronDefinitionEntity fromCronDefinition(CronDefinition definition) {
        return CronDefinitionEntity.builder()
                .id(definition.getId())
                .name(definition.getName())
                .description(definition.getDescription())
                .createdBy(definition.getCreatedBy())
                .cronType(definition.getCronType() != null ? definition.getCronType().name() : null)
                .cronExpression(definition.getCronExpression())
                .timezone(definition.getTimezone())
                .startTime(definition.getStartTime())
                .endTime(definition.getEndTime())
                .maxConcurrent(definition.getMaxConcurrent())
                .jobType(definition.getJobType() != null ? definition.getJobType().name() : null)
                .jobHandler(definition.getJobHandler())
                .jobData(definition.getJobData())
                .misfirePolicy(definition.getMisfirePolicy() != null ? definition.getMisfirePolicy().name() : null)
                .timeoutMs(definition.getTimeoutMs())
                .retryCount(definition.getRetryCount())
                .retryIntervalMs(definition.getRetryIntervalMs())
                .status(definition.getStatus() != null ? definition.getStatus().name() : null)
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .version(definition.getVersion())
                .build();
    }
}

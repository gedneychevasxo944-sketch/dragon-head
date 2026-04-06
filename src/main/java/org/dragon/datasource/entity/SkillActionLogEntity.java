package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.actionlog.ActionDetail;
import org.dragon.skill.actionlog.SkillActionLog;
import org.dragon.skill.enums.SkillActionType;

import java.time.LocalDateTime;

/**
 * SkillActionLogEntity — 映射 skill_action_log 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "skill_action_log")
public class SkillActionLogEntity {

    @Id
    private String id;

    @Column(name = "skill_id", nullable = false, length = 64)
    private String skillId;

    @Column(name = "skill_name", length = 100)
    private String skillName;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column
    private Integer version;

    @Column(columnDefinition = "JSON")
    @DbJson
    private ActionDetail detail;

    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /**
     * 转换为 SkillActionLog 领域对象。
     */
    public SkillActionLog toDomain() {
        return SkillActionLog.builder()
                .id(this.id)
                .skillId(this.skillId)
                .skillName(this.skillName)
                .actionType(this.actionType != null
                        ? SkillActionType.valueOf(this.actionType) : null)
                .operatorId(this.operatorId)
                .operatorName(this.operatorName)
                .version(this.version)
                .detail(this.detail)
                .createdAt(this.createdAt)
                .build();
    }

    /**
     * 从 SkillActionLog 领域对象创建 Entity。
     */
    public static SkillActionLogEntity fromDomain(SkillActionLog log) {
        return SkillActionLogEntity.builder()
                .id(log.getId())
                .skillId(log.getSkillId())
                .skillName(log.getSkillName())
                .actionType(log.getActionType() != null ? log.getActionType().name() : null)
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .version(log.getVersion())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

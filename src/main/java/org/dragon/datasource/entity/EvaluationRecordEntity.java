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
import java.util.List;
import java.util.Map;

import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.ObservationFinding;
import java.util.List;
import java.util.Map;

/**
 * EvaluationRecordEntity 评价记录实体
 * 映射数据库 evaluation_record 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evaluation_record")
public class EvaluationRecordEntity {

    @Id
    private String id;

    private String targetType;

    private String targetId;

    private String taskId;

    private String planId;

    private String evaluationType;

    @DbJson
    private List<String> dimensions;

    @DbJson
    private List<ObservationFinding> findings;

    @DbJson
    private List<String> unsafeFlags;

    @DbJson
    private List<String> evidenceRefs;

    private Double taskCompletionScore;

    private Double efficiencyScore;

    private Double complianceScore;

    private Double collaborationScore;

    private Double satisfactionScore;

    private Double overallScore;

    private String analysis;

    @DbJson
    private List<String> suggestions;

    private Double confidence;

    @DbJson
    private Map<String, Object> evidence;

    private LocalDateTime timestamp;

    private String evaluator;

    @DbJson
    private Map<String, Object> extensions;

    /**
     * 转换为EvaluationRecord
     */
    public EvaluationRecord toEvaluationRecord() {
        return EvaluationRecord.builder()
                .id(this.id)
                .targetType(this.targetType != null ? EvaluationRecord.TargetType.valueOf(this.targetType) : null)
                .targetId(this.targetId)
                .taskId(this.taskId)
                .planId(this.planId)
                .evaluationType(this.evaluationType != null ? EvaluationRecord.EvaluationType.valueOf(this.evaluationType) : null)
                .dimensions(this.dimensions)
                .findings(this.findings)
                .unsafeFlags(this.unsafeFlags)
                .evidenceRefs(this.evidenceRefs)
                .taskCompletionScore(this.taskCompletionScore)
                .efficiencyScore(this.efficiencyScore)
                .complianceScore(this.complianceScore)
                .collaborationScore(this.collaborationScore)
                .satisfactionScore(this.satisfactionScore)
                .overallScore(this.overallScore)
                .analysis(this.analysis)
                .suggestions(this.suggestions)
                .confidence(this.confidence)
                .evidence(this.evidence)
                .timestamp(this.timestamp)
                .evaluator(this.evaluator)
                .extensions(this.extensions)
                .build();
    }

    /**
     * 从EvaluationRecord创建Entity
     */
    public static EvaluationRecordEntity fromEvaluationRecord(EvaluationRecord record) {
        return EvaluationRecordEntity.builder()
                .id(record.getId())
                .targetType(record.getTargetType() != null ? record.getTargetType().name() : null)
                .targetId(record.getTargetId())
                .taskId(record.getTaskId())
                .planId(record.getPlanId())
                .evaluationType(record.getEvaluationType() != null ? record.getEvaluationType().name() : null)
                .dimensions(record.getDimensions())
                .findings(record.getFindings())
                .unsafeFlags(record.getUnsafeFlags())
                .evidenceRefs(record.getEvidenceRefs())
                .taskCompletionScore(record.getTaskCompletionScore())
                .efficiencyScore(record.getEfficiencyScore())
                .complianceScore(record.getComplianceScore())
                .collaborationScore(record.getCollaborationScore())
                .satisfactionScore(record.getSatisfactionScore())
                .overallScore(record.getOverallScore())
                .analysis(record.getAnalysis())
                .suggestions(record.getSuggestions())
                .confidence(record.getConfidence())
                .evidence(record.getEvidence())
                .timestamp(record.getTimestamp())
                .evaluator(record.getEvaluator())
                .extensions(record.getExtensions())
                .build();
    }
}

package org.dragon.observer.evaluation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dragon.observer.collector.dto.ObservationDataset;
import org.dragon.observer.collector.dto.CharacterObservationSnapshot;
import org.dragon.observer.collector.dto.WorkspaceObservationSnapshot;
import org.dragon.observer.collector.dto.MemoryObservationSnapshot;
import org.dragon.observer.collector.dto.SkillObservationSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * EvaluationEngine 评价引擎
 * 执行评价逻辑，支持规则评价和模型驱动评价
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class EvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EvaluationEngine.class);

    /**
     * 任务执行数据
     */
    public static class TaskData {
        private String taskId;
        private String targetId;
        private EvaluationRecord.TargetType targetType;
        private String taskInput;
        private String taskOutput;
        private Long durationMs;
        private Integer tokensUsed;
        private Boolean success;
        private String errorMessage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Map<String, Object> metadata;

        // Getters and setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
        public EvaluationRecord.TargetType getTargetType() { return targetType; }
        public void setTargetType(EvaluationRecord.TargetType targetType) { this.targetType = targetType; }
        public String getTaskInput() { return taskInput; }
        public void setTaskInput(String taskInput) { this.taskInput = taskInput; }
        public String getTaskOutput() { return taskOutput; }
        public void setTaskOutput(String taskOutput) { this.taskOutput = taskOutput; }
        public Long getDurationMs() { return durationMs; }
        public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
        public Integer getTokensUsed() { return tokensUsed; }
        public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    private final EvaluationRecordStore evaluationRecordStore;

    /**
     * 评价配置
     */
    public static class EvaluationConfig {
        // 任务完成度权重
        private double taskCompletionWeight = 0.3;
        // 效率权重
        private double efficiencyWeight = 0.2;
        // 合规性权重
        private double complianceWeight = 0.2;
        // 协作表现权重
        private double collaborationWeight = 0.15;
        // 用户满意度权重
        private double satisfactionWeight = 0.15;

        // 标准阈值
        private long maxDurationMs = 60000; // 60秒
        private int maxTokens = 100000;

        // Getters and setters
        public double getTaskCompletionWeight() { return taskCompletionWeight; }
        public void setTaskCompletionWeight(double taskCompletionWeight) { this.taskCompletionWeight = taskCompletionWeight; }
        public double getEfficiencyWeight() { return efficiencyWeight; }
        public void setEfficiencyWeight(double efficiencyWeight) { this.efficiencyWeight = efficiencyWeight; }
        public double getComplianceWeight() { return complianceWeight; }
        public void setComplianceWeight(double complianceWeight) { this.complianceWeight = complianceWeight; }
        public double getCollaborationWeight() { return collaborationWeight; }
        public void setCollaborationWeight(double collaborationWeight) { this.collaborationWeight = collaborationWeight; }
        public double getSatisfactionWeight() { return satisfactionWeight; }
        public void setSatisfactionWeight(double satisfactionWeight) { this.satisfactionWeight = satisfactionWeight; }
        public long getMaxDurationMs() { return maxDurationMs; }
        public void setMaxDurationMs(long maxDurationMs) { this.maxDurationMs = maxDurationMs; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    }

    private final EvaluationConfig config = new EvaluationConfig();

    /**
     * 基于规则评价任务
     *
     * @param taskData 任务数据
     * @return 评价记录
     */
    public EvaluationRecord evaluateByRules(TaskData taskData) {
        log.info("[EvaluationEngine] Evaluating task by rules: {}", taskData.getTaskId());

        EvaluationRecord record = EvaluationRecord.builder()
                .id(UUID.randomUUID().toString())
                .taskId(taskData.getTaskId())
                .targetId(taskData.getTargetId())
                .targetType(taskData.getTargetType())
                .evaluationType(EvaluationRecord.EvaluationType.TASK)
                .timestamp(LocalDateTime.now())
                .evaluator("OBSERVER")
                .build();

        // 1. 评价任务完成度
        double taskCompletionScore = evaluateTaskCompletion(taskData);
        record.setTaskCompletionScore(taskCompletionScore);

        // 2. 评价效率
        double efficiencyScore = evaluateEfficiency(taskData);
        record.setEfficiencyScore(efficiencyScore);

        // 3. 评价合规性
        double complianceScore = evaluateCompliance(taskData);
        record.setComplianceScore(complianceScore);

        // 4. 协作表现（默认满分，因为单任务评价）
        record.setCollaborationScore(1.0);

        // 5. 用户满意度（暂无外部反馈，默认满分）
        record.setSatisfactionScore(1.0);

        // 计算综合评分
        record.calculateOverallScore();

        // 生成分析和建议
        record.setAnalysis(generateAnalysis(record));
        record.setSuggestions(generateSuggestions(record));

        // 设置证据
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("durationMs", taskData.getDurationMs());
        evidence.put("tokensUsed", taskData.getTokensUsed());
        evidence.put("success", taskData.getSuccess());
        record.setEvidence(evidence);

        // 保存记录
        evaluationRecordStore.save(record);

        log.info("[EvaluationEngine] Evaluation completed: overallScore={}", record.getOverallScore());
        return record;
    }

    /**
     * 周期性评价
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 评价记录
     */
    public EvaluationRecord evaluatePeriodically(
            EvaluationRecord.TargetType targetType, String targetId,
            LocalDateTime startTime, LocalDateTime endTime) {

        log.info("[EvaluationEngine] Periodic evaluation for {}: {}", targetType, targetId);

        // 获取时间范围内的任务评价记录
        List<EvaluationRecord> records = evaluationRecordStore.findByTargetAndTimeRange(
                targetType, targetId, startTime, endTime);

        if (records.isEmpty()) {
            log.warn("[EvaluationEngine] No records found for periodic evaluation");
            return null;
        }

        // 计算平均分
        double avgTaskCompletion = records.stream()
                .mapToDouble(r -> r.getTaskCompletionScore() != null ? r.getTaskCompletionScore() : 0)
                .average().orElse(0);

        double avgEfficiency = records.stream()
                .mapToDouble(r -> r.getEfficiencyScore() != null ? r.getEfficiencyScore() : 0)
                .average().orElse(0);

        double avgCompliance = records.stream()
                .mapToDouble(r -> r.getComplianceScore() != null ? r.getComplianceScore() : 0)
                .average().orElse(0);

        double avgCollaboration = records.stream()
                .mapToDouble(r -> r.getCollaborationScore() != null ? r.getCollaborationScore() : 0)
                .average().orElse(0);

        double avgSatisfaction = records.stream()
                .mapToDouble(r -> r.getSatisfactionScore() != null ? r.getSatisfactionScore() : 0)
                .average().orElse(0);

        EvaluationRecord record = EvaluationRecord.builder()
                .id(UUID.randomUUID().toString())
                .targetId(targetId)
                .targetType(targetType)
                .evaluationType(EvaluationRecord.EvaluationType.PERIODIC)
                .taskCompletionScore(avgTaskCompletion)
                .efficiencyScore(avgEfficiency)
                .complianceScore(avgCompliance)
                .collaborationScore(avgCollaboration)
                .satisfactionScore(avgSatisfaction)
                .timestamp(LocalDateTime.now())
                .evaluator("OBSERVER")
                .build();

        record.calculateOverallScore();
        record.setAnalysis(generateAnalysis(record));
        record.setSuggestions(generateSuggestions(record));

        // 设置证据
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("periodStart", startTime.toString());
        evidence.put("periodEnd", endTime.toString());
        evidence.put("recordCount", records.size());
        record.setEvidence(evidence);

        evaluationRecordStore.save(record);

        log.info("[EvaluationEngine] Periodic evaluation completed: overallScore={}", record.getOverallScore());
        return record;
    }

    /**
     * 评价任务完成度
     */
    private double evaluateTaskCompletion(TaskData taskData) {
        if (taskData.getSuccess() == null || !taskData.getSuccess()) {
            return 0.0;
        }

        // 简单判断：有输出且无错误即为完成
        if (taskData.getTaskOutput() != null && !taskData.getTaskOutput().isEmpty()) {
            return 1.0;
        }

        return 0.5;
    }

    /**
     * 评价效率
     */
    private double evaluateEfficiency(TaskData taskData) {
        // 耗时评分
        double durationScore = 1.0;
        if (taskData.getDurationMs() != null) {
            if (taskData.getDurationMs() > config.getMaxDurationMs()) {
                durationScore = 0.0;
            } else {
                durationScore = 1.0 - ((double) taskData.getDurationMs() / config.getMaxDurationMs());
            }
        }

        // Token 评分
        double tokenScore = 1.0;
        if (taskData.getTokensUsed() != null) {
            if (taskData.getTokensUsed() > config.getMaxTokens()) {
                tokenScore = 0.0;
            } else {
                tokenScore = 1.0 - ((double) taskData.getTokensUsed() / config.getMaxTokens());
            }
        }

        return (durationScore + tokenScore) / 2;
    }

    /**
     * 评价合规性
     */
    private double evaluateCompliance(TaskData taskData) {
        // 简单规则：无错误消息即为合规
        if (taskData.getErrorMessage() != null && !taskData.getErrorMessage().isEmpty()) {
            return 0.0;
        }
        return 1.0;
    }

    /**
     * 生成分析内容
     */
    private String generateAnalysis(EvaluationRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("综合评分: ").append(String.format("%.2f", record.getOverallScore())).append("\n");

        if (record.getTaskCompletionScore() != null) {
            sb.append("任务完成度: ").append(String.format("%.2f", record.getTaskCompletionScore()));
            if (record.getTaskCompletionScore() < 0.6) {
                sb.append(" (需改进)");
            }
            sb.append("\n");
        }

        if (record.getEfficiencyScore() != null) {
            sb.append("效率: ").append(String.format("%.2f", record.getEfficiencyScore()));
            if (record.getEfficiencyScore() < 0.6) {
                sb.append(" (需改进)");
            }
            sb.append("\n");
        }

        if (record.getComplianceScore() != null) {
            sb.append("合规性: ").append(String.format("%.2f", record.getComplianceScore()));
            if (record.getComplianceScore() < 0.6) {
                sb.append(" (需改进)");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 生成改进建议
     */
    private List<String> generateSuggestions(EvaluationRecord record) {
        List<String> suggestions = new ArrayList<>();

        if (record.getTaskCompletionScore() != null && record.getTaskCompletionScore() < 0.6) {
            suggestions.add("建议检查任务执行逻辑，确保输出正确");
        }

        if (record.getEfficiencyScore() != null && record.getEfficiencyScore() < 0.6) {
            suggestions.add("建议优化执行策略，减少耗时和资源消耗");
        }

        if (record.getComplianceScore() != null && record.getComplianceScore() < 0.6) {
            suggestions.add("建议检查错误处理机制，确保合规执行");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("表现良好，继续保持");
        }

        return suggestions;
    }

    public EvaluationConfig getConfig() {
        return config;
    }

    /**
     * 基于观测数据集的多维度评价
     *
     * @param dataset 观测数据集
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 评价记录（包含多维度发现）
     */
    public EvaluationRecord evaluateWithFindings(ObservationDataset dataset,
            EvaluationRecord.TargetType targetType, String targetId) {
        log.info("[EvaluationEngine] Multi-dimension evaluation for {}: {}", targetType, targetId);

        EvaluationRecord record = EvaluationRecord.builder()
                .id(UUID.randomUUID().toString())
                .targetId(targetId)
                .targetType(targetType)
                .evaluationType(EvaluationRecord.EvaluationType.PERIODIC)
                .timestamp(LocalDateTime.now())
                .evaluator("OBSERVER")
                .dimensions(new ArrayList<>())
                .findings(new ArrayList<>())
                .unsafeFlags(new ArrayList<>())
                .evidenceRefs(new ArrayList<>())
                .build();

        List<String> dimensions = new ArrayList<>();
        List<ObservationFinding> findings = new ArrayList<>();

        // 1. 任务完成度维度
        dimensions.add("TASK_COMPLETION");
        double taskScore = evaluateTaskCompletionDimension(dataset, targetType, targetId);
        record.setTaskCompletionScore(taskScore);

        if (taskScore < 0.6) {
            findings.add(ObservationFinding.builder()
                    .dimension("TASK_COMPLETION")
                    .severity(taskScore < 0.3 ? "HIGH" : "MEDIUM")
                    .summary("任务完成度偏低")
                    .details(String.format("当前得分: %.2f", taskScore))
                    .targetType(targetType.name())
                    .targetId(targetId)
                    .suggestedActionType("UPDATE_MIND")
                    .confidence(taskScore)
                    .unsafe(false)
                    .evidence(new HashMap<>())
                    .build());
        }

        // 2. 效率维度
        dimensions.add("EFFICIENCY");
        double efficiencyScore = evaluateEfficiencyDimension(dataset, targetType, targetId);
        record.setEfficiencyScore(efficiencyScore);

        if (efficiencyScore < 0.6) {
            findings.add(ObservationFinding.builder()
                    .dimension("EFFICIENCY")
                    .severity(efficiencyScore < 0.3 ? "HIGH" : "MEDIUM")
                    .summary("执行效率偏低")
                    .details(String.format("当前得分: %.2f", efficiencyScore))
                    .targetType(targetType.name())
                    .targetId(targetId)
                    .suggestedActionType("UPDATE_CONFIG")
                    .confidence(efficiencyScore)
                    .unsafe(false)
                    .evidence(new HashMap<>())
                    .build());
        }

        // 3. 合规性维度
        dimensions.add("COMPLIANCE");
        double complianceScore = evaluateComplianceDimension(dataset, targetType, targetId);
        record.setComplianceScore(complianceScore);

        if (complianceScore < 0.6) {
            findings.add(ObservationFinding.builder()
                    .dimension("COMPLIANCE")
                    .severity(complianceScore < 0.3 ? "HIGH" : "MEDIUM")
                    .summary("合规性问题")
                    .details(String.format("当前得分: %.2f", complianceScore))
                    .targetType(targetType.name())
                    .targetId(targetId)
                    .suggestedActionType("UPDATE_PERSONALITY")
                    .confidence(complianceScore)
                    .unsafe(false)
                    .evidence(new HashMap<>())
                    .build());
        }

        // 4. 协作维度
        dimensions.add("COLLABORATION");
        double collaborationScore = evaluateCollaborationDimension(dataset, targetType, targetId);
        record.setCollaborationScore(collaborationScore);

        if (collaborationScore < 0.6) {
            findings.add(ObservationFinding.builder()
                    .dimension("COLLABORATION")
                    .severity(collaborationScore < 0.3 ? "HIGH" : "MEDIUM")
                    .summary("协作能力有待提升")
                    .details(String.format("当前得分: %.2f", collaborationScore))
                    .targetType(targetType.name())
                    .targetId(targetId)
                    .suggestedActionType("UPDATE_MIND")
                    .confidence(collaborationScore)
                    .unsafe(false)
                    .evidence(new HashMap<>())
                    .build());
        }

        // 5. 满意度维度
        dimensions.add("SATISFACTION");
        double satisfactionScore = evaluateSatisfactionDimension(dataset, targetType, targetId);
        record.setSatisfactionScore(satisfactionScore);

        if (satisfactionScore < 0.6) {
            findings.add(ObservationFinding.builder()
                    .dimension("SATISFACTION")
                    .severity(satisfactionScore < 0.3 ? "HIGH" : "MEDIUM")
                    .summary("满意度偏低")
                    .details(String.format("当前得分: %.2f", satisfactionScore))
                    .targetType(targetType.name())
                    .targetId(targetId)
                    .suggestedActionType("UPDATE_MEMORY")
                    .confidence(satisfactionScore)
                    .unsafe(false)
                    .evidence(new HashMap<>())
                    .build());
        }

        // 设置维度和发现
        record.setDimensions(dimensions);
        record.setFindings(findings);

        // 计算综合评分
        record.calculateOverallScore();

        // 生成分析和建议
        record.setAnalysis(generateAnalysis(record));
        record.setSuggestions(generateSuggestions(record));

        // 保存记录
        evaluationRecordStore.save(record);

        log.info("[EvaluationEngine] Multi-dimension evaluation completed: overallScore={}, findings={}",
                record.getOverallScore(), findings.size());
        return record;
    }

    private double evaluateTaskCompletionDimension(ObservationDataset dataset,
            EvaluationRecord.TargetType targetType, String targetId) {
        if (dataset.getRawTaskData() == null || dataset.getRawTaskData().isEmpty()) {
            return 1.0;
        }

        long totalTasks = dataset.getRawTaskData().size();
        long completedTasks = dataset.getRawTaskData().stream()
                .filter(t -> Boolean.TRUE.equals(t.get("success")))
                .count();

        return totalTasks > 0 ? (double) completedTasks / totalTasks : 1.0;
    }

    private double evaluateEfficiencyDimension(ObservationDataset dataset,
            EvaluationRecord.TargetType targetType, String targetId) {
        if (dataset.getRawTaskData() == null || dataset.getRawTaskData().isEmpty()) {
            return 1.0;
        }

        double avgDuration = dataset.getRawTaskData().stream()
                .filter(t -> t.get("durationMs") != null)
                .mapToLong(t -> ((Number) t.get("durationMs")).longValue())
                .average()
                .orElse(0.0);

        // 低于30秒为满分，超过5分钟为0分
        if (avgDuration <= 0) return 1.0;
        if (avgDuration > 300000) return 0.0;
        return 1.0 - (avgDuration / 300000);
    }

    private double evaluateComplianceDimension(ObservationDataset dataset,
            EvaluationRecord.TargetType targetType, String targetId) {
        // 基于目标类型的快照进行合规性评估
        switch (targetType) {
            case CHARACTER:
                if (dataset.getCharacterSnapshot() != null) {
                    return dataset.getCharacterSnapshot().getSuccessRate();
                }
                break;
            case WORKSPACE:
                if (dataset.getWorkspaceSnapshot() != null) {
                    return dataset.getWorkspaceSnapshot().getSuccessRate();
                }
                break;
            default:
                break;
        }
        return 1.0;
    }

    private double evaluateCollaborationDimension(ObservationDataset dataset,
            EvaluationRecord.TargetType targetType, String targetId) {
        if (targetType != EvaluationRecord.TargetType.WORKSPACE) {
            return 1.0;
        }

        WorkspaceObservationSnapshot wsSnapshot = dataset.getWorkspaceSnapshot();
        if (wsSnapshot == null) {
            return 1.0;
        }

        // 基于活跃角色比例评估协作
        if (wsSnapshot.getCharacterCount() == 0) {
            return 1.0;
        }

        return (double) wsSnapshot.getActiveCharacterCount() / wsSnapshot.getCharacterCount();
    }

    private double evaluateSatisfactionDimension(ObservationDataset dataset,
            EvaluationRecord.TargetType targetType, String targetId) {
        // 暂无外部反馈机制，默认满分
        return 1.0;
    }
}

package org.dragon.observer.optimization;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dragon.character.mind.Mind;
import org.dragon.character.mind.PersonalityDescriptor;
import org.dragon.character.mind.tag.Tag;
import org.dragon.workspace.commons.CommonSenseValidator;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.EvaluationRecordStore;
import org.dragon.observer.log.ModificationLog;
import org.dragon.observer.log.ModificationLogStore;
import org.dragon.observer.optimization.applier.OptimizationTargetApplier;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.store.OptimizationActionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * OptimizationExecutor 优化执行器
 * 负责执行优化动作，通过策略模式将动作分派到对应的 OptimizationTargetApplier
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class OptimizationExecutor {

    private static final Logger log = LoggerFactory.getLogger(OptimizationExecutor.class);

    private final OptimizationActionStore optimizationActionStore;
    private final ModificationLogStore modificationLogStore;
    private final CommonSenseValidator commonSenseValidator;
    private final EvaluationRecordStore evaluationRecordStore;
    private final List<OptimizationTargetApplier> appliers;
    private final LLMSuggestionGenerator suggestionGenerator;

    /**
     * 获取指定目标类型的 applier
     */
    private OptimizationTargetApplier getApplier(OptimizationAction.TargetType targetType) {
        return appliers.stream()
                .filter(a -> a.getTargetType() == targetType)
                .findFirst()
                .orElse(null);
    }

    /**
     * 执行优化动作
     */
    public OptimizationAction execute(String actionId) {
        OptimizationAction action = optimizationActionStore.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Optimization action not found: " + actionId));
        return execute(action);
    }

    /**
     * 执行优化动作
     */
    public OptimizationAction execute(OptimizationAction action) {
        if (!action.canExecute()) {
            log.warn("[OptimizationExecutor] Action cannot execute: {}", action.getId());
            action.markRejected("Action is not in PENDING state");
            optimizationActionStore.save(action);
            return action;
        }

        // 常识校验
        var validationResult = commonSenseValidator.validate(
                action.getTargetType().name(),
                action.getActionType().name(),
                action.getParameters());

        if (!validationResult.isValid()) {
            String reason = "Violated common sense: " + validationResult.getViolations();
            log.warn("[OptimizationExecutor] Action rejected by common sense: {}", reason);
            action.markRejected(reason);
            optimizationActionStore.save(action);
            return action;
        }

        OptimizationTargetApplier applier = getApplier(action.getTargetType());
        if (applier == null) {
            log.warn("[OptimizationExecutor] No applier found for target type: {}", action.getTargetType());
            action.markRejected("No applier for target type: " + action.getTargetType());
            optimizationActionStore.save(action);
            return action;
        }

        try {
            // 保存修改前的快照
            String beforeSnapshot = applier.captureSnapshot(action.getTargetId());

            // 执行修改
            OptimizationTargetApplier.ApplyResult result = applier.apply(action);

            if (!result.isSuccess()) {
                action.markRejected(result.getError());
                optimizationActionStore.save(action);
                return action;
            }

            // 保存修改后的快照
            String afterSnapshot = applier.captureSnapshot(action.getTargetId());

            // 记录修改日志
            ModificationLog modLog = ModificationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .targetType(ModificationLog.TargetType.valueOf(action.getTargetType().name()))
                    .targetId(action.getTargetId())
                    .beforeSnapshot(beforeSnapshot)
                    .afterSnapshot(afterSnapshot)
                    .triggerSource(ModificationLog.TriggerSource.OBSERVER)
                    .evaluationId(action.getEvaluationId())
                    .reason(generateReason(action))
                    .operator("OBSERVER")
                    .timestamp(LocalDateTime.now())
                    .build();
            modificationLogStore.save(modLog);

            // 标记为已执行
            action.markExecuted(result.getMessage());
            optimizationActionStore.save(action);

            log.info("[OptimizationExecutor] Action executed successfully: {}", action.getId());

        } catch (Exception e) {
            log.error("[OptimizationExecutor] Action execution failed: {}", action.getId(), e);
            action.setStatus(OptimizationAction.Status.FAILED);
            action.setResult("Execution failed: " + e.getMessage());
            optimizationActionStore.save(action);
        }

        return action;
    }

    /**
     * 批量执行优化动作
     */
    public List<OptimizationAction> executeBatch(List<String> actionIds) {
        List<OptimizationAction> results = new ArrayList<>();
        for (String actionId : actionIds) {
            results.add(execute(actionId));
        }
        return results;
    }

    /**
     * 回滚优化动作
     */
    public OptimizationAction rollback(String actionId) {
        OptimizationAction action = optimizationActionStore.findById(actionId)
                .orElseThrow(() -> new IllegalArgumentException("Optimization action not found: " + actionId));

        if (!action.canRollback()) {
            log.warn("[OptimizationExecutor] Action cannot rollback: {}", action.getId());
            return action;
        }

        OptimizationTargetApplier applier = getApplier(action.getTargetType());
        if (applier == null) {
            log.warn("[OptimizationExecutor] No applier found for target type: {}", action.getTargetType());
            return action;
        }

        try {
            if (action.getBeforeSnapshot() != null) {
                applier.restoreFromSnapshot(action.getTargetId(), action.getBeforeSnapshot());

                ModificationLog rollbackLog = ModificationLog.builder()
                        .id(UUID.randomUUID().toString())
                        .targetType(ModificationLog.TargetType.valueOf(action.getTargetType().name()))
                        .targetId(action.getTargetId())
                        .beforeSnapshot(action.getAfterSnapshot())
                        .afterSnapshot(action.getBeforeSnapshot())
                        .triggerSource(ModificationLog.TriggerSource.OBSERVER)
                        .evaluationId(action.getEvaluationId())
                        .reason("Rollback of action: " + action.getId())
                        .operator("OBSERVER")
                        .timestamp(LocalDateTime.now())
                        .build();
                modificationLogStore.save(rollbackLog);
            }

            action.markRolledBack();
            optimizationActionStore.save(action);

            log.info("[OptimizationExecutor] Action rolled back successfully: {}", action.getId());

        } catch (Exception e) {
            log.error("[OptimizationExecutor] Action rollback failed: {}", action.getId(), e);
            action.setResult("Rollback failed: " + e.getMessage());
            optimizationActionStore.save(action);
        }

        return action;
    }

    /**
     * 根据评价生成优化动作
     */
    public List<OptimizationAction> generateActionsFromEvaluation(String evaluationId) {
        List<OptimizationAction> actions = new ArrayList<>();

        EvaluationRecord evaluation = evaluationRecordStore.findById(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found: " + evaluationId));

        if (evaluation.getSuggestions() != null) {
            for (String suggestion : evaluation.getSuggestions()) {
                OptimizationAction action = generateAction(evaluation, suggestion);
                if (action != null) {
                    actions.add(action);
                    optimizationActionStore.save(action);
                }
            }
        }

        return actions;
    }

    /**
     * 从建议生成优化动作
     */
    private OptimizationAction generateAction(EvaluationRecord evaluation, String suggestion) {
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("suggestion", suggestion);

        OptimizationAction.ActionType actionType;
        if (suggestion.contains("任务执行")) {
            actionType = OptimizationAction.ActionType.UPDATE_MIND;
        } else if (suggestion.contains("效率")) {
            actionType = OptimizationAction.ActionType.UPDATE_CONFIG;
        } else if (suggestion.contains("技能")) {
            actionType = OptimizationAction.ActionType.ADD_SKILL;
        } else {
            actionType = OptimizationAction.ActionType.UPDATE_PERSONALITY;
        }

        return OptimizationAction.builder()
                .id(UUID.randomUUID().toString())
                .evaluationId(evaluation.getId())
                .targetType(evaluation.getTargetType() == EvaluationRecord.TargetType.CHARACTER
                        ? OptimizationAction.TargetType.CHARACTER
                        : OptimizationAction.TargetType.WORKSPACE)
                .targetId(evaluation.getTargetId())
                .actionType(actionType)
                .parameters(params)
                .priority(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 使用 LLM 生成的建议执行优化
     */
    public OptimizationAction executeWithLLM(OptimizationAction action) {
        if (!action.canExecute()) {
            log.warn("[OptimizationExecutor] Action cannot execute: {}", action.getId());
            action.markRejected("Action is not in PENDING state");
            optimizationActionStore.save(action);
            return action;
        }

        OptimizationTargetApplier applier = getApplier(action.getTargetType());
        if (applier == null) {
            log.warn("[OptimizationExecutor] No applier found for target type: {}", action.getTargetType());
            action.markRejected("No applier for target type: " + action.getTargetType());
            optimizationActionStore.save(action);
            return action;
        }

        try {
            String workspace = (String) action.getParameters().get("workspace");
            String organizationId = (String) action.getParameters().get("organizationId");

            List<String> suggestions = suggestionGenerator.generateSuggestions(
                    workspace,
                    organizationId,
                    action.getTargetType(),
                    action.getTargetId(),
                    10);

            if (suggestions.isEmpty()) {
                log.info("[OptimizationExecutor] No suggestions generated, skipping optimization");
                action.markExecuted("No suggestions generated");
                optimizationActionStore.save(action);
                return action;
            }

            // 保存修改前的快照
            String beforeSnapshot = applier.captureSnapshot(action.getTargetId());

            // 直接通过 applier 应用（以 UPDATE_MIND 形式）
            action.getParameters().put("suggestions", suggestions);
            OptimizationTargetApplier.ApplyResult result = applier.apply(action);

            if (!result.isSuccess()) {
                action.markRejected(result.getError());
                optimizationActionStore.save(action);
                return action;
            }

            // 保存修改后的快照
            String afterSnapshot = applier.captureSnapshot(action.getTargetId());

            ModificationLog modLog = ModificationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .targetType(ModificationLog.TargetType.valueOf(action.getTargetType().name()))
                    .targetId(action.getTargetId())
                    .beforeSnapshot(beforeSnapshot)
                    .afterSnapshot(afterSnapshot)
                    .triggerSource(ModificationLog.TriggerSource.OBSERVER)
                    .evaluationId(action.getEvaluationId())
                    .reason("LLM-driven optimization with suggestions: " + suggestions)
                    .operator("OBSERVER_LLM")
                    .timestamp(LocalDateTime.now())
                    .build();
            modificationLogStore.save(modLog);

            action.markExecuted("LLM-driven optimization executed with " + suggestions.size() + " suggestions");
            action.getParameters().put("suggestions", suggestions);
            optimizationActionStore.save(action);

            log.info("[OptimizationExecutor] LLM-driven optimization executed successfully: {} suggestions", suggestions.size());

        } catch (Exception e) {
            log.error("[OptimizationExecutor] LLM-driven optimization failed: {}", action.getId(), e);
            action.setStatus(OptimizationAction.Status.FAILED);
            action.setResult("LLM optimization failed: " + e.getMessage());
            optimizationActionStore.save(action);
        }

        return action;
    }

    /**
     * 生成修改原因
     */
    private String generateReason(OptimizationAction action) {
        return String.format("Optimization triggered by evaluation %s, action type: %s",
                action.getEvaluationId(), action.getActionType());
    }
}
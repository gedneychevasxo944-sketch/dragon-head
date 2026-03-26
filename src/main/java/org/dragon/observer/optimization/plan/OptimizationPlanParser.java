package org.dragon.observer.optimization.plan;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dragon.observer.evaluation.ObservationFinding;
import org.dragon.observer.optimization.plan.OptimizationAction.ActionType;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;

/**
 * OptimizationPlanParser 优化计划解析器
 * 解析 LLM 生成的优化计划文本为结构化的 OptimizationPlan
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class OptimizationPlanParser {

    private static final Logger log = LoggerFactory.getLogger(OptimizationPlanParser.class);
    private final Gson gson = new Gson();

    // 动作类型匹配模式
    private static final Map<String, OptimizationAction.ActionType> ACTION_TYPE_PATTERNS = Map.of(
            "更新心智", OptimizationAction.ActionType.UPDATE_MIND,
            "更新性格", OptimizationAction.ActionType.UPDATE_PERSONALITY,
            "添加技能", OptimizationAction.ActionType.ADD_SKILL,
            "移除技能", OptimizationAction.ActionType.REMOVE_SKILL,
            "调整权重", OptimizationAction.ActionType.ADJUST_WEIGHT,
            "更新记忆", OptimizationAction.ActionType.UPDATE_MEMORY,
            "更新标签", OptimizationAction.ActionType.UPDATE_TAG,
            "更新配置", OptimizationAction.ActionType.UPDATE_CONFIG
    );

    // 序号匹配模式：1. 或 1) 或 【1】
    private static final Pattern ITEM_PATTERN = Pattern.compile("(?:^|[\\n\\r])([\\d]+)[.、\\)．]\\s*(.+)");

    /**
     * 解析优化计划文本
     *
     * @param planText LLM 生成的计划文本
     * @param observerId Observer ID
     * @param evaluationId 关联的评价记录 ID
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 解析后的优化计划
     */
    public OptimizationPlan parse(String planText, String observerId, String evaluationId,
            OptimizationAction.TargetType targetType, String targetId) {
        log.info("[OptimizationPlanParser] Parsing optimization plan text, length: {}", planText.length());

        OptimizationPlan plan = OptimizationPlan.builder()
                .id(UUID.randomUUID().toString())
                .observerId(observerId)
                .evaluationId(evaluationId)
                .targetType(targetType)
                .targetId(targetId)
                .status(OptimizationPlan.Status.DRAFT)
                .rawContent(planText)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 解析计划项目
        List<OptimizationPlanItem> items = parseItems(planText, plan.getId());
        plan.setItems(items);

        // 生成摘要
        plan.setSummary(buildSummary(planText, items));

        log.info("[OptimizationPlanParser] Parsed plan with {} items", items.size());
        return plan;
    }

    /**
     * 从评价记录和发现列表生成计划
     *
     * @param evaluationId 评价记录 ID
     * @param findings 观察发现列表
     * @param observerId Observer ID
     * @param targetType 目标类型
     * @param targetId 目标 ID
     * @return 解析后的优化计划
     */
    public OptimizationPlan parseFromFindings(String evaluationId, List<ObservationFinding> findings,
            String observerId, OptimizationAction.TargetType targetType, String targetId) {
        log.info("[OptimizationPlanParser] Parsing plan from {} findings", findings != null ? findings.size() : 0);

        OptimizationPlan plan = OptimizationPlan.builder()
                .id(UUID.randomUUID().toString())
                .observerId(observerId)
                .evaluationId(evaluationId)
                .targetType(targetType)
                .targetId(targetId)
                .status(OptimizationPlan.Status.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<OptimizationPlanItem> items = new ArrayList<>();

        if (findings != null) {
            int sequence = 1;
            for (ObservationFinding finding : findings) {
                OptimizationAction.ActionType actionType = inferActionTypeFromFinding(finding);
                OptimizationPlanItem item = OptimizationPlanItem.builder()
                        .id(UUID.randomUUID().toString())
                        .planId(plan.getId())
                        .sequence(sequence++)
                        .actionType(actionType)
                        .targetId(targetId)
                        .description(finding.getSummary() + ": " + finding.getDetails())
                        .parameters(gson.toJson(Map.of(
                                "dimension", finding.getDimension(),
                                "severity", finding.getSeverity(),
                                "confidence", finding.getConfidence(),
                                "suggestedAction", finding.getSuggestedActionType()
                        )))
                        .status(OptimizationPlanItem.Status.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                items.add(item);
            }
        }

        plan.setItems(items);
        plan.setSummary(buildSummaryFromFindings(items));

        return plan;
    }

    /**
     * 从发现推断动作类型
     */
    private OptimizationAction.ActionType inferActionTypeFromFinding(ObservationFinding finding) {
        String suggestedAction = finding.getSuggestedActionType();
        if (suggestedAction != null) {
            try {
                return OptimizationAction.ActionType.valueOf(suggestedAction);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        String dimension = finding.getDimension();
        if (dimension == null) {
            return OptimizationAction.ActionType.UPDATE_MIND;
        }

        return switch (dimension) {
            case "TASK_COMPLETION" -> OptimizationAction.ActionType.UPDATE_MIND;
            case "EFFICIENCY" -> OptimizationAction.ActionType.UPDATE_CONFIG;
            case "COMPLIANCE" -> OptimizationAction.ActionType.UPDATE_PERSONALITY;
            case "COLLABORATION" -> OptimizationAction.ActionType.UPDATE_MIND;
            case "SATISFACTION" -> OptimizationAction.ActionType.UPDATE_MEMORY;
            default -> OptimizationAction.ActionType.UPDATE_MIND;
        };
    }

    /**
     * 解析计划文本中的项目
     */
    private List<OptimizationPlanItem> parseItems(String planText, String planId) {
        List<OptimizationPlanItem> items = new ArrayList<>();
        String[] lines = planText.split("[\\n\\r]+");

        int sequence = 1;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 尝试匹配序号模式
            Matcher matcher = ITEM_PATTERN.matcher(line);
            if (matcher.find()) {
                String content = matcher.group(2).trim();
                OptimizationAction.ActionType actionType = inferActionType(content);
                String description = cleanDescription(content);

                OptimizationPlanItem item = OptimizationPlanItem.builder()
                        .id(UUID.randomUUID().toString())
                        .planId(planId)
                        .sequence(sequence++)
                        .actionType(actionType)
                        .targetId(null) // 将在后续设置
                        .description(description)
                        .parameters("{}")
                        .status(OptimizationPlanItem.Status.PENDING)
                        .createdAt(LocalDateTime.now())
                        .build();
                items.add(item);
            }
        }

        // 如果没有解析到项目，创建一个默认项目
        if (items.isEmpty()) {
            OptimizationPlanItem item = OptimizationPlanItem.builder()
                    .id(UUID.randomUUID().toString())
                    .planId(planId)
                    .sequence(1)
                    .actionType(OptimizationAction.ActionType.UPDATE_MIND)
                    .targetId(null)
                    .description(planText.length() > 200 ? planText.substring(0, 200) + "..." : planText)
                    .parameters("{}")
                    .status(OptimizationPlanItem.Status.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            items.add(item);
        }

        return items;
    }

    /**
     * 从描述推断动作类型
     */
    private OptimizationAction.ActionType inferActionType(String description) {
        String lower = description.toLowerCase();

        for (var entry : ACTION_TYPE_PATTERNS.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // 默认返回 UPDATE_MIND
        return OptimizationAction.ActionType.UPDATE_MIND;
    }

    /**
     * 清理描述文本
     */
    private String cleanDescription(String content) {
        // 移除可能的动作类型前缀
        for (String prefix : ACTION_TYPE_PATTERNS.keySet()) {
            if (content.startsWith(prefix) || content.startsWith("【" + prefix + "】")) {
                content = content.substring(prefix.length()).trim();
                break;
            }
        }
        // 移除可能的目标 ID 前缀
        content = content.replaceAll("^(?:目标|Title|对象)：?\\s*", "");
        // 截断过长的描述
        if (content.length() > 500) {
            content = content.substring(0, 500) + "...";
        }
        return content.trim();
    }

    /**
     * 从文本构建摘要
     */
    private String buildSummary(String planText, List<OptimizationPlanItem> items) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("基于文本解析，生成 %d 个优化项目。\n", items.size()));
        if (!items.isEmpty()) {
            summary.append("优化项目：\n");
            for (int i = 0; i < items.size(); i++) {
                OptimizationPlanItem item = items.get(i);
                summary.append(String.format("%d. [%s] %s\n", i + 1, item.getActionType(), item.getDescription()));
            }
        }
        return summary.toString();
    }

    /**
     * 从发现构建摘要
     */
    private String buildSummaryFromFindings(List<OptimizationPlanItem> items) {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("基于 %d 个观察发现，生成优化项目。\n", items.size()));
        if (!items.isEmpty()) {
            summary.append("优化项目：\n");
            for (int i = 0; i < items.size(); i++) {
                OptimizationPlanItem item = items.get(i);
                summary.append(String.format("%d. [%s] %s\n", i + 1, item.getActionType(), item.getDescription()));
            }
        }
        return summary.toString();
    }

    /**
     * 验证计划文本是否有效
     *
     * @param planText 计划文本
     * @return 是否有效
     */
    public boolean isValidPlanText(String planText) {
        if (planText == null || planText.trim().isEmpty()) {
            return false;
        }
        // 至少包含一些中文字符
        return planText.trim().length() >= 10;
    }
}

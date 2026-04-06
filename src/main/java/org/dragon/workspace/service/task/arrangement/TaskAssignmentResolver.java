package org.dragon.workspace.service.task.arrangement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.character.Character;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.character.builtin.BuiltInCharacterFactory;
import org.dragon.workspace.service.task.arrangement.dto.PromptWriterInput;
import org.dragon.workspace.service.task.arrangement.dto.AssignmentDecision;
import org.dragon.workspace.service.task.arrangement.dto.ChildTaskPlan;
import org.dragon.workspace.service.task.arrangement.dto.TaskDecompositionResult;
import org.dragon.agent.llm.util.CharacterCaller;
import org.dragon.config.PromptKeys;
import org.dragon.config.service.ConfigApplication;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务分配解析器
 * 负责在任务分解后，为每个子任务选择合适的执行者
 * 通过 PromptWriter 生成选择提示，再由 MemberSelector Character 执行选择
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAssignmentResolver {

    private final BuiltInCharacterFactory builtInCharacterFactory;
    private final CharacterCaller characterCaller;
    private final ConfigApplication configApplication;
    private final Gson gson = new Gson();

    /**
     * 解析任务分配
     * 为分解结果中的每个子任务选择合适的成员
     *
     * @param result 任务分解结果
     * @param workspace 工作空间
     * @param members 可用成员列表
     * @return 更新后的分解结果（包含已填充的 characterId）
     */
    public TaskDecompositionResult resolveAssignments(
            TaskDecompositionResult result,
            Workspace workspace,
            List<WorkspaceMember> members) {

        if (result == null || result.getChildTasks() == null || result.getChildTasks().isEmpty()) {
            log.warn("[TaskAssignmentResolver] No child tasks to assign");
            return result;
        }

        // 获取 PromptWriter Character 用于生成选择提示
        Character promptWriter = builtInCharacterFactory.getOrCreatePromptWriterCharacter(workspace.getId());

        // 获取 MemberSelector Character 用于执行选择
        Character memberSelector = builtInCharacterFactory.getOrCreateMemberSelectorCharacter(workspace.getId());

        // 获取选择提示模板
        String promptTemplate = configApplication.getGlobalPrompt(
                PromptKeys.MEMBER_SELECTOR_SELECT,
                "请从可用成员中选择最合适的执行者来完成指定任务。");

        // 收集已被选中的成员（避免重复分配）
        List<String> excludedCharacterIds = new ArrayList<>();
        List<AssignmentDecision> decisions = new ArrayList<>();

        // 处理每个子任务
        List<String> unassignedIds = new ArrayList<>();
        Map<String, String> failures = new HashMap<>();

        for (ChildTaskPlan plan : result.getChildTasks()) {
            // 跳过已分配的
            if (plan.getCharacterId() != null && !plan.getCharacterId().isEmpty()) {
                log.info("[TaskAssignmentResolver] Plan {} already has characterId: {}",
                        plan.getPlanTaskId(), plan.getCharacterId());
                excludedCharacterIds.add(plan.getCharacterId());
                continue;
            }

            try {
                AssignmentDecision decision = selectMemberForTask(
                        plan, workspace, members, promptWriter, memberSelector,
                        promptTemplate, excludedCharacterIds);

                if (decision != null && decision.getSelectedCharacterId() != null) {
                    plan.setCharacterId(decision.getSelectedCharacterId());
                    plan.setCharacterRole(decision.getSelectedMemberRole());
                    excludedCharacterIds.add(decision.getSelectedCharacterId());
                    decisions.add(decision);
                    log.info("[TaskAssignmentResolver] Assigned plan {} to character {} ({})",
                            plan.getPlanTaskId(), decision.getSelectedCharacterId(),
                            decision.getSelectedMemberRole());
                } else {
                    unassignedIds.add(plan.getPlanTaskId());
                    failures.put(plan.getPlanTaskId(), "No suitable member found");
                    log.warn("[TaskAssignmentResolver] Failed to assign plan {}: no suitable member",
                            plan.getPlanTaskId());
                }
            } catch (Exception e) {
                log.error("[TaskAssignmentResolver] Failed to assign plan {}: {}",
                        plan.getPlanTaskId(), e.getMessage());
                unassignedIds.add(plan.getPlanTaskId());
                failures.put(plan.getPlanTaskId(), e.getMessage());
            }
        }

        log.info("[TaskAssignmentResolver] Selection completed: {} assigned, {} unassigned",
                decisions.size(), unassignedIds.size());

        return result;
    }

    /**
     * 为单个任务选择成员
     */
    private AssignmentDecision selectMemberForTask(
            ChildTaskPlan plan,
            Workspace workspace,
            List<WorkspaceMember> members,
            Character promptWriter,
            Character memberSelector,
            String promptTemplate,
            List<String> excludedCharacterIds) {

        // 构建 PromptWriter 输入
        PromptWriterInput promptWriterInput = buildPromptWriterInput(
                plan, workspace, members, promptTemplate, excludedCharacterIds);

        // 调用 PromptWriter 生成完整提示词
        String selectionPrompt = characterCaller.call(promptWriter, gson.toJson(promptWriterInput));

        // 调用 MemberSelector 执行选择
        String rawResult = characterCaller.call(memberSelector, selectionPrompt);

        if (rawResult == null || rawResult.isEmpty() || "NONE".equalsIgnoreCase(rawResult.trim())) {
            return null;
        }

        String selectedId = rawResult.trim();

        // 查找选中的成员
        WorkspaceMember selectedMember = members.stream()
                .filter(m -> m.getCharacterId().equals(selectedId))
                .findFirst()
                .orElse(null);

        if (selectedMember == null) {
            log.warn("[TaskAssignmentResolver] Selected character {} not found in members", selectedId);
            return null;
        }

        return AssignmentDecision.builder()
                .planTaskId(plan.getPlanTaskId())
                .selectedCharacterId(selectedId)
                .selectedMemberRole(selectedMember.getRole())
                .selectionReason("Selected based on capability matching")
                .confidence(0.8)
                .decisionAt(LocalDateTime.now())
                .build();
    }

    /**
     * 构建 PromptWriter 输入
     */
    private PromptWriterInput buildPromptWriterInput(
            ChildTaskPlan plan,
            Workspace workspace,
            List<WorkspaceMember> members,
            String promptTemplate,
            List<String> excludedCharacterIds) {

        // 构建成员信息
        List<PromptWriterInput.MemberInfo> memberInfos = members.stream()
                .filter(m -> !excludedCharacterIds.contains(m.getCharacterId()))
                .map(m -> {
                    String capability = m.getTags() != null && !m.getTags().isEmpty()
                            ? String.join(", ", m.getTags())
                            : null;
                    String description = buildMemberDescription(m);
                    return PromptWriterInput.MemberInfo.builder()
                            .characterId(m.getCharacterId())
                            .role(m.getRole())
                            .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                            .capability(capability)
                            .description(description)
                            .build();
                })
                .collect(Collectors.toList());

        // 构建任务信息
        PromptWriterInput.TaskInfo taskInfo = PromptWriterInput.TaskInfo.builder()
                .id(plan.getPlanTaskId())
                .name(plan.getName())
                .description(plan.getDescription())
                .build();

        // 构建上下文提示
        Map<String, Object> contextHints = new HashMap<>();
        contextHints.put("expectedOutput", plan.getExpectedOutput());
        contextHints.put("excludeCharacterIds", excludedCharacterIds);
        contextHints.put("dependencyPlanTaskIds", plan.getDependencyPlanTaskIds());

        return PromptWriterInput.builder()
                .workspaceId(workspace.getId())
                .promptType("member_selection")
                .promptTemplate(promptTemplate)
                .task(taskInfo)
                .members(memberInfos)
                .contextHints(contextHints)
                .allowFollowUp(false)
                .build();
    }

    /**
     * 构建成员描述
     */
    private String buildMemberDescription(WorkspaceMember m) {
        StringBuilder sb = new StringBuilder();
        if (m.getRole() != null) {
            sb.append("角色: ").append(m.getRole());
        }
        if (m.getLayer() != null) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("层级: ").append(m.getLayer());
        }
        if (m.getTags() != null && !m.getTags().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append("标签: ").append(String.join(", ", m.getTags()));
        }
        return sb.toString();
    }
}

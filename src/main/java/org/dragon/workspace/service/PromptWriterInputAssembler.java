package org.dragon.workspace.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dragon.task.Task;
import org.dragon.workspace.built_ins.character.prompt_writer.dto.PromptWriterInput;
import org.dragon.workspace.member.WorkspaceMember;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

/**
 * PromptWriter 输入组装器
 * 负责将任务和成员信息组装为 PromptWriter 可用的 JSON 输入
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class PromptWriterInputAssembler {

    private final Gson gson = new Gson();

    /**
     * 构建任务分解用的 PromptWriter 输入
     */
    public String buildTaskDecomposeInput(String promptType, String promptTemplate,
            Task task, List<WorkspaceMember> members) {
        List<PromptWriterInput.MemberInfo> memberInfos = members.stream()
                .map(m -> {
                    // 构建 capability：使用 tags 列表
                    String capability = m.getTags() != null && !m.getTags().isEmpty()
                            ? String.join(", ", m.getTags())
                            : null;
                    // 构建 description：综合 role、layer、tags
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

        // 构建成员能力描述和协作约束
        Map<String, Object> memberCapabilities = members.stream()
                .collect(Collectors.toMap(
                        WorkspaceMember::getCharacterId,
                        m -> m.getTags() != null ? m.getTags() : List.of()
                ));
        Map<String, Object> memberDescriptions = members.stream()
                .collect(Collectors.toMap(
                        WorkspaceMember::getCharacterId,
                        this::buildMemberDescription
                ));

        Map<String, Object> contextHints = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "collaborationMode", "AUTO",
                "allowFollowUp", true,
                "maxChildTasks", 10,
                "memberCapabilities", memberCapabilities,
                "memberDescriptions", memberDescriptions,
                "collaborationConstraint", "子任务间应保持独立性，按依赖关系顺序执行"
        );

        PromptWriterInput input = PromptWriterInput.builder()
                .workspaceId(task.getWorkspaceId())
                .promptType(promptType)
                .promptTemplate(promptTemplate)
                .task(PromptWriterInput.TaskInfo.builder()
                        .id(task.getId())
                        .name(task.getName())
                        .description(task.getDescription())
                        .input(task.getInput())
                        .parentTaskId(task.getParentTaskId())
                        .build())
                .members(memberInfos)
                .contextHints(contextHints)
                .build();

        return gson.toJson(input);
    }

    /**
     * 构建成员描述
     * 综合 role、layer、tags 生成描述文本
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

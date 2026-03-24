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
                .map(m -> PromptWriterInput.MemberInfo.builder()
                        .characterId(m.getCharacterId())
                        .role(m.getRole())
                        .layer(m.getLayer() != null ? m.getLayer().toString() : null)
                        .build())
                .collect(Collectors.toList());

        Map<String, Object> contextHints = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "collaborationMode", "AUTO",
                "allowFollowUp", true,
                "maxChildTasks", 10
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
}

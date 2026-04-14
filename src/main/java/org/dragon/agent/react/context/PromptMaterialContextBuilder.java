package org.dragon.agent.react.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.config.CharacterExecutorConfig;
import org.dragon.character.mind.Mind;
import org.dragon.skill.util.SkillDirectoryBuilder;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.task.Task;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.member.WorkspaceMember;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prompt 物料上下文构建器
 * <p>
 * 职责：根据配置收集并填充 {@link PromptMaterialContext} 的各个部分。
 * 所有 Prompt 物料的收集逻辑集中在此，便于维护和调优。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptMaterialContextBuilder {

    private final WorkspaceRegistry workspaceRegistry;
    private final SkillRegistry skillRegistry;

    /**
     * 构建 ReAct 执行所需的完整 PromptMaterialContext
     */
    public PromptMaterialContext buildForReAct(
            String workspaceId,
            String characterId,
            Task task,
            Mind mind,
            List<SkillDefinition> activeSkills,
            CharacterExecutorConfig.ReActConfig reActConfig
    ) {
        return buildForReAct(workspaceId, characterId, task, mind, activeSkills, reActConfig,
                PromptMaterialConfig.defaultReAct());
    }

    /**
     * 构建 ReAct 执行所需的 PromptMaterialContext（带配置）
     */
    public PromptMaterialContext buildForReAct(
            String workspaceId,
            String characterId,
            Task task,
            Mind mind,
            List<SkillDefinition> activeSkills,
            CharacterExecutorConfig.ReActConfig reActConfig,
            PromptMaterialConfig config
    ) {
        PromptMaterialContext.PromptMaterialContextBuilder builder = PromptMaterialContext.builder()
                .maxIterations(reActConfig != null ? reActConfig.getMaxIterations() : 10);

        // 1. Workspace 级别（根据开关）
        if (workspaceId != null) {
            fillWorkspaceContext(builder, workspaceId, config);
        }

        // 2. Character 级别
        if (config.isIncludeCharacterPersonality() && mind != null && mind.getPersonality() != null) {
            builder.characterPersonality(mind.getPersonality());
        }
        if (config.isIncludeAvailableSkills() && skillRegistry != null && characterId != null) {
            List<SkillDefinition> skills = skillRegistry.getSkills(characterId, workspaceId);
            builder.availableSkills(skills);
            builder.skillDirectoryPrompt(SkillDirectoryBuilder.buildDirectoryPrompt(skills));
        }

        // 3. Task 级别
        if (config.isIncludeTaskContext() && task != null) {
            fillTaskContext(builder, task);
        }

        // 4. Evaluation（根据开关）
        if (config.isIncludeRecentEvaluation() || config.isIncludeRecentFindings()) {
            fillEvaluationContext(builder, workspaceId, characterId, config);
        }

        // 5. Memory（根据开关）
        if (config.isIncludeRecentMemories()) {
            fillMemoryContext(builder, characterId);
        }

        return builder.build();
    }

    /**
     * 构建成员选择所需的 PromptMaterialContext
     */
    public PromptMaterialContext buildForMemberSelection(
            String workspaceId,
            Task parentTask,
            List<WorkspaceMember> members
    ) {
        return buildForMemberSelection(workspaceId, parentTask, members,
                PromptMaterialConfig.memberSelection());
    }

    public PromptMaterialContext buildForMemberSelection(
            String workspaceId,
            Task parentTask,
            List<WorkspaceMember> members,
            PromptMaterialConfig config
    ) {
        PromptMaterialContext.PromptMaterialContextBuilder builder = PromptMaterialContext.builder();

        if (workspaceId != null) {
            fillWorkspaceContext(builder, workspaceId, config);
        }
        if (parentTask != null) {
            fillTaskContext(builder, parentTask);
        }
        builder.availableMembers(members);

        return builder.build();
    }

    /**
     * 构建任务分解所需的 PromptMaterialContext
     */
    public PromptMaterialContext buildForTaskDecomposition(
            String workspaceId,
            Task parentTask,
            List<WorkspaceMember> members
    ) {
        return buildForTaskDecomposition(workspaceId, parentTask, members,
                PromptMaterialConfig.taskDecomposition());
    }

    public PromptMaterialContext buildForTaskDecomposition(
            String workspaceId,
            Task parentTask,
            List<WorkspaceMember> members,
            PromptMaterialConfig config
    ) {
        PromptMaterialContext.PromptMaterialContextBuilder builder = PromptMaterialContext.builder();

        if (workspaceId != null) {
            fillWorkspaceContext(builder, workspaceId, config);
        }
        if (parentTask != null) {
            fillTaskContext(builder, parentTask);
        }
        builder.availableMembers(members);

        return builder.build();
    }

    // ========== 私有填充方法 ==========

    private void fillWorkspaceContext(
            PromptMaterialContext.PromptMaterialContextBuilder builder,
            String workspaceId,
            PromptMaterialConfig config
    ) {
        workspaceRegistry.get(workspaceId).ifPresent(workspace -> {
            if (config.isIncludeWorkspaceBasicInfo()) {
                builder.workspaceName(workspace.getName())
                       .workspaceDescription(workspace.getDescription())
                       .workspaceStatus(workspace.getStatus());
            }
            if (config.isIncludeWorkspacePersonality()) {
                builder.workspacePersonality(workspace.getPersonality());
            }
        });

        // TODO: 填充 availableMembers（需要 WorkspaceMemberService）
        // TODO: 填充 teamPositions（需要 TeamPositionService）
    }

    private void fillTaskContext(PromptMaterialContext.PromptMaterialContextBuilder builder, Task task) {
        builder.taskId(task.getId())
               .taskName(task.getName())
               .taskDescription(task.getDescription())
               .taskInput(task.getInput() != null ? task.getInput().toString() : null)
               .parentTaskId(task.getParentTaskId())
               .childTaskIds(task.getChildTaskIds());
    }

    private void fillEvaluationContext(
            PromptMaterialContext.PromptMaterialContextBuilder builder,
            String workspaceId,
            String characterId,
            PromptMaterialConfig config
    ) {
        // TODO: 从 EvaluationRecordStore 获取最近评估记录
        // 暂时留空，后续集成时实现
        log.debug("[PromptMaterialContextBuilder] Evaluation context filling not yet implemented");
    }

    private void fillMemoryContext(
            PromptMaterialContext.PromptMaterialContextBuilder builder,
            String characterId
    ) {
        // TODO: 从 MemoryStore 获取最近 Memory
        // 暂时留空，后续集成时实现
        log.debug("[PromptMaterialContextBuilder] Memory context filling not yet implemented");
    }
}

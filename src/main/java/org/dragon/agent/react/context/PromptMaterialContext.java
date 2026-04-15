package org.dragon.agent.react.context;

import lombok.Builder;
import lombok.Data;
import org.dragon.character.mind.PersonalityDescriptor;
import org.dragon.character.mind.memory.Memory;
import org.dragon.observer.evaluation.EvaluationRecord;
import org.dragon.observer.evaluation.ObservationFinding;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.workspace.Workspace;
import org.dragon.workspace.WorkspacePersonality;
import org.dragon.workspace.member.WorkspaceMember;
import org.dragon.workspace.task.dto.TaskDecompositionResult;

import java.util.List;
import java.util.Map;

/**
 * Prompt 物料上下文 - 存放所有与 Prompt 生成相关的物料
 * <p>
 * 在 LLM 调用前，通过 PromptMaterialContextBuilder 收集并填充所有相关物料，
 * 便于统一管理和调优。
 */
@Data
@Builder
public class PromptMaterialContext {

    // ========== Workspace 级别 ==========

    /**
     * Workspace 人格（workingStyle, decisionPattern, riskTolerance, coreValues 等）
     */
    private WorkspacePersonality workspacePersonality;

    /**
     * 可用成员列表（用于成员选择）
     */
    private List<WorkspaceMember> availableMembers;

    /**
     * Workspace 基本信息
     */
    private String workspaceName;
    private String workspaceDescription;
    private Workspace.Status workspaceStatus;

    // ========== Character 级别 ==========

    /**
     * Character 人格（Mind.getPersonality().toPrompt()）
     */
    private PersonalityDescriptor characterPersonality;

    /**
     * 可用 Skill 列表
     */
    private List<SkillDefinition> availableSkills;

    /**
     * Skill 目录 Prompt（已组装好的）
     */
    private String skillDirectoryPrompt;

    // ========== Task 级别 ==========

    /**
     * 当前任务信息
     */
    private String taskId;
    private String taskName;
    private String taskDescription;
    private String taskInput;
    private String parentTaskId;
    private List<String> childTaskIds;

    /**
     * 任务分解结果（如果已分解）
     */
    private TaskDecompositionResult decompositionResult;

    // ========== Evaluation/Observability ==========

    /**
     * 最近评估记录
     */
    private EvaluationRecord recentEvaluation;

    /**
     * 评估发现列表
     */
    private List<ObservationFinding> recentFindings;

    // ========== Memory ==========

    /**
     * 最近 Memory（用于 MEMORY action）
     */
    private List<Memory> recentMemories;

    // ========== Collaboration ==========

    /**
     * 协作会话 ID
     */
    private String collaborationSessionId;

    /**
     * 同级 Character IDs
     */
    private List<String> peerCharacterIds;

    /**
     * 参与方状态
     */
    private Map<String, String> participantStates;

    // ========== Execution Metadata ==========

    /**
     * 最大迭代次数
     */
    private int maxIterations;

    /**
     * 当前迭代次数
     */
    private int currentIteration;

    /**
     * 是否启用流式
     */
    private boolean streamingEnabled;
}

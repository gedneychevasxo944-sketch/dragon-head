package org.dragon.skill.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 执行上下文。
 *
 * <p>由 Agent 框架在每次对话时构建，传入 ToolContext，供 SkillTool 使用：
 * <ul>
 *   <li>SkillLoader 根据 characterId + workspaceId 加载该上下文下可用的 Skill 集合</li>
 *   <li>agentId 用于 fork 模式追踪父子 Agent 关系，以及 invokedSkills 的归属</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {

    /** 当前执行的 Character ID */
    private String characterId;

    /**
     * 当前所在的 Workspace ID。
     * 为 null 表示 Character 在独立（无 Workspace）模式下执行。
     */
    private String workspaceId;

    /**
     * 当前 Agent 实例 ID（UUID）。
     * fork 模式下用于追踪父子 Agent 关系；同时作为 invokedSkills 在 session 中的归属 key。
     */
    private String agentId;
}


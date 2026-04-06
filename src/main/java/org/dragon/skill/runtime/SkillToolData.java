package org.dragon.skill.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SkillTool 执行结果的结构化载荷，通过 {@code ToolResult.data} 字段携带。
 *
 * <p>框架层（Agent 主循环）识别到 data 是 SkillToolData 时，负责：
 * <ol>
 *   <li>将 {@code newMessages} 注入当前对话（inline 模式）</li>
 *   <li>应用 {@code contextPatch}（allowedTools/model/effort 变更）</li>
 *   <li>若 {@code persistContent} 非空，调用 session.addPersistedSkill()（设计点 6）</li>
 * </ol>
 *
 * <p>对应 TS 版本 {@code SkillTool.call()} 返回的 {@code { data, newMessages, contextModifier }}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillToolData {

    /** 调用的技能名称 */
    private String skillName;

    /** 执行模式 */
    private ExecutionMode executionMode;

    /**
     * 上下文变更声明（设计点 5）。
     * 框架层应用，SkillTool 不直接修改 AgentContext。
     */
    private ContextPatch contextPatch;

    /**
     * inline 模式下需要注入到对话的新消息列表（设计点 2 - prompt 内容注入）。
     * 元素类型由框架的 Message 类型决定，此处用 Object 保持通用性。
     * fork 模式下为 null（结果通过 ToolResult.output 携带）。
     */
    private List<Object> newMessages;

    /**
     * 需要持续留存的内容（设计点 6）。
     * 非 null 时，框架层调用 session.addPersistedSkill(skillName, persistContent)。
     * 内容由 persistMode 决定：full=完整正文，summary=约束片段。
     */
    private String persistContent;

    /** 执行模式枚举 */
    public enum ExecutionMode {
        /** 在当前对话中直接展开 prompt */
        INLINE,
        /** 创建独立 sub-AgentTask 执行 */
        FORK
    }
}


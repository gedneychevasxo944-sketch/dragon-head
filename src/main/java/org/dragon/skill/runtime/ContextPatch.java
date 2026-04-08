package org.dragon.skill.runtime;

import org.dragon.skill.enums.SkillEffort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SkillTool 执行后对 AgentContext 的变更声明（设计点 5）。
 *
 * <p>SkillTool 自身不直接操作 AgentContext，而是通过返回 ContextPatch
 * 声明"我希望修改哪些上下文"，由框架层（Agent 主循环）负责应用。
 * 这样 SkillTool 不依赖具体的上下文实现，职责更清晰且便于测试。
 *
 * <p>对应 TS 版本的 {@code contextModifier} 模式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextPatch {

    /**
     * 本次 Skill 执行期间额外允许使用的工具列表（设计点 5 - allowedTools 注入）。
     * 追加到当前 Agent 的已有 allowedTools，Skill 执行结束后由框架恢复原状态。
     * null/空 表示不修改工具权限。
     */
    private List<String> additionalAllowedTools;

    /**
     * 覆盖本次对话使用的模型。
     * null 表示不覆盖，保持当前 Agent 的默认模型。
     */
    private String modelOverride;

    /**
     * 覆盖本次对话的 effort 级别。
     * null 表示不覆盖。
     */
    private SkillEffort effortOverride;

    /**
     * Skill 执行工作目录的绝对路径（由 SkillWorkspaceManager 物化后注入）。
     *
     * <p>框架层（Agent 主循环）在执行 Bash/脚本类工具时，应将此路径作为 cwd 或
     * 追加到沙箱的文件系统挂载点，使 Skill 包内的相对路径引用能够正确解析。
     * null 表示该 Skill 无附属文件，无需切换工作目录。
     */
    private String skillWorkDir;

    /** 是否为空补丁（所有字段都为空） */
    public boolean isEmpty() {
        return (additionalAllowedTools == null || additionalAllowedTools.isEmpty())
                && modelOverride == null
                && effortOverride == null
                && skillWorkDir == null;
    }

    /** 快速构建只带 allowedTools 的补丁 */
    public static ContextPatch ofTools(List<String> tools) {
        return ContextPatch.builder().additionalAllowedTools(tools).build();
    }
}


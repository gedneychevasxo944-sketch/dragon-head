package org.dragon.skill.domain;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.SkillEffort;
import lombok.Data;

import java.util.List;

/**
 * Skill 运行时配置。
 *
 * <p>包含执行行为相关的配置项，序列化为 JSON 后存储在 runtime_config 字段。
 *
 * <p>字段说明：
 * <ul>
 *   <li>whenToUse: 使用场景说明，告诉模型何时自动调用此 skill</li>
 *   <li>argumentHint: 参数提示，如 "[scope] <branch>"</li>
 *   <li>aliases: 别名列表</li>
 *   <li>allowedTools: 允许的工具列表</li>
 *   <li>model: 指定模型（覆盖默认）</li>
 *   <li>disableModelInvocation: 是否禁用模型自动调用</li>
 *   <li>userInvocable: 用户是否可通过 /name 手动调用</li>
 *   <li>executionContext: 执行上下文（inline/fork）</li>
 *   <li>effort: 努力程度</li>
 * </ul>
 */
@Data
public class SkillRuntimeConfig {

    /** 使用场景说明 */
    private String whenToUse;

    /** 参数提示 */
    private String argumentHint;

    /** 别名列表 */
    private List<String> aliases;

    /** 允许的工具列表 */
    private List<String> allowedTools;

    /** 指定模型（覆盖默认） */
    private String model;

    /** 是否禁用模型自动调用 */
    private Boolean disableModelInvocation;

    /** 用户是否可通过 /name 手动调用 */
    private Boolean userInvocable;

    /** 执行上下文 */
    private ExecutionContext executionContext;

    /** 努力程度 */
    private SkillEffort effort;

    /** 是否持续留存上下文 */
    private Boolean persist;

    /** 留存模式 */
    private org.dragon.skill.enums.PersistMode persistMode;
}

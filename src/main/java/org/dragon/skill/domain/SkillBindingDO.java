package org.dragon.skill.domain;

import org.dragon.skill.enums.BindingType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SkillBindingDO — skill_bindings 表领域对象。
 *
 * <pre>
 * bindingType         | characterId | workspaceId | 场景
 * --------------------+-------------+-------------+-------------------------------
 * CHARACTER           | 必填        | NULL        | Character 自有 skill
 * WORKSPACE           | NULL        | 必填        | Workspace 公共 skill 池
 * CHARACTER_WORKSPACE | 必填        | 必填        | Character 在某 Workspace 的专属 skill
 * </pre>
 *
 * <p>绑定关系绑定到 Skill 本身，具体使用哪个版本由 skill.publishedVersionId 决定。
 */
@Data
public class SkillBindingDO {

    /** 物理自增主键 */
    private Long id;

    /** 绑定关系类型 */
    private BindingType bindingType;

    /**
     * Character 主键（来自 characters 表）。
     * bindingType = CHARACTER 或 CHARACTER_WORKSPACE 时必填，否则为 NULL。
     */
    private String characterId;

    /**
     * Workspace 主键（来自 workspaces 表）。
     * bindingType = WORKSPACE 或 CHARACTER_WORKSPACE 时必填，否则为 NULL。
     */
    private String workspaceId;

    /** 技能唯一标识（UUID），对应 skills.skillId */
    private String skillId;

    /** 绑定创建时间 */
    private LocalDateTime createdAt;

    /** 绑定最后更新时间 */
    private LocalDateTime updatedAt;
}

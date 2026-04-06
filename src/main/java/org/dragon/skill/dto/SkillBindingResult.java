package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 绑定/解绑 Skill 操作的返回结果。
 */
@Data
@AllArgsConstructor
public class SkillBindingResult {

    /** 绑定记录的物理主键 */
    private Long bindingId;

    /** 技能唯一标识（UUID） */
    private String skillId;
}

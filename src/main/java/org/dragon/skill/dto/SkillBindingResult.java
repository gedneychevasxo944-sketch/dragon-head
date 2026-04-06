package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 绑定/解绑 Skill 操作的返回结果。
 */
@Data
@AllArgsConstructor
public class SkillBindingResult {

    /** 绑定记录的物理主键（可用于后续解绑操作） */
    private Long bindingId;

    /** 绑定类型：character / workspace / character_workspace */
    private String bindingType;

    /** 技能唯一标识（UUID） */
    private String skillId;

    /** 版本类型：latest / fixed */
    private String versionType;

    /**
     * 实际绑定的版本号。
     * <ul>
     *   <li>versionType = 'latest'：此处返回绑定时当前最新 active 版本号（仅供展示，非锁定）</li>
     *   <li>versionType = 'fixed'：此处返回 fixedVersion 指定的版本号</li>
     * </ul>
     */
    private Integer resolvedVersion;
}


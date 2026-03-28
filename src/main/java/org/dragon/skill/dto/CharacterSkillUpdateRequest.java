package org.dragon.skill.dto;

import lombok.Data;

/**
 * 更新 Character Skill 关联配置请求。
 *
 * @since 1.0
 */
@Data
public class CharacterSkillUpdateRequest {

    /** 更新版本跟随策略 */
    private Boolean useLatest;

    /**
     * 若 useLatest=false，指定锁定的版本号。
     */
    private Integer pinnedVersion;

    /** 启用/禁用此 skill */
    private Boolean enabled;
}
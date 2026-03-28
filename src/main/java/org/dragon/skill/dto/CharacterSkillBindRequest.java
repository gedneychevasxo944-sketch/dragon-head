package org.dragon.skill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Character 绑定 Skill 请求。
 *
 * @since 1.0
 */
@Data
public class CharacterSkillBindRequest {

    @NotNull
    private Long skillId;

    /**
     * 目标 workspace ID。
     * - null：character 全局绑定（跨所有 workspace）
     * - 有值：character + workspace 组合绑定
     */
    private Long workspaceId;

    /**
     * 是否跟随最新版本，默认 true。
     */
    private Boolean useLatest = true;

    /**
     * 若 useLatest=false，需指定锁定的版本号。
     */
    private Integer pinnedVersion;
}
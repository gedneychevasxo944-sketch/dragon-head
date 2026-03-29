package org.dragon.skill.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * Skill 绑定请求（统一维度，支持 Workspace 和 Character）。
 *
 * - BindType.WORKSPACE：workspaceId 标识目标 workspace，characterId 为 null
 * - BindType.CHARACTER：characterId 标识目标 character，workspaceId 为 null（全局绑定）
 * - BindType.CHARACTER_WORKSPACE：characterId + workspaceId 组合标识
 *
 * @since 1.0
 */
@Data
public class SkillBindingRequest {

    @NotNull
    private Long skillId;

    /**
     * 目标 workspace ID。
     * - workspace 圈选：必填
     * - character 全局绑定：null
     * - character + workspace 绑定：有值
     */
    private Long workspaceId;

    /**
     * 是否跟随最新版本，默认 true。
     */
    private Boolean useLatest = true;

    /**
     * 若 useLatest=false，需指定锁定的版本号。
     * 若 useLatest=true，此字段忽略，系统自动使用当前最新版本。
     */
    private Integer pinnedVersion;
}

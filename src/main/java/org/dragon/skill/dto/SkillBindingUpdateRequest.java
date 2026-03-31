package org.dragon.skill.dto;

import lombok.Data;

/**
 * 更新 Skill 绑定配置请求（Workspace 维度）。
 *
 * @since 1.0
 */
@Data
public class SkillBindingUpdateRequest {

    /** 更新版本跟随策略 */
    private Boolean useLatest;

    /**
     * 若 useLatest=false，指定锁定的版本号。
     * 版本号必须 <= skill 当前最新版本。
     */
    private Integer pinnedVersion;

    /** 在当前 workspace 中启用/禁用此 skill */
    private Boolean enabled;

    /**
     * 检查是否使用最新版本
     */
    public boolean isUseLatest() {
        return Boolean.TRUE.equals(useLatest);
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}

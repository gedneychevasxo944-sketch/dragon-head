package org.dragon.skill.dto;

import lombok.Data;

/**
 * 更新 Workspace Skill 关联配置请求。
 *
 * @since 1.0
 */
@Data
public class WorkspaceSkillUpdateRequest {

    /** 更新版本跟随策略 */
    private Boolean useLatest;

    /**
     * 若 useLatest=false，指定锁定的版本号。
     * 版本号必须 <= skill 当前最新版本。
     */
    private Integer pinnedVersion;

    /** 在当前 workspace 中启用/禁用此 skill */
    private Boolean enabled;
}
package org.dragon.skill.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * Workspace 圈选 Skill 请求。
 *
 * @since 1.0
 */
@Data
public class WorkspaceSkillBindRequest {

    @NotNull
    private Long skillId;

    /**
     * 是否跟随最新版本，默认 true。
     */
    private Boolean useLatest = true;

    /**
     * 若 useLatest=false，需指定锁定的版本号。
     * 若 useLatest=true，此字段忽略，系统自动使用当前最新版本。
     */
    private Integer pinnedVersion;

    /**
     * 检查是否使用最新版本
     */
    public boolean isUseLatest() {
        return Boolean.TRUE.equals(useLatest);
    }
}
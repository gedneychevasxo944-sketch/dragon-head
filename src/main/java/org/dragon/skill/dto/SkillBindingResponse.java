package org.dragon.skill.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Skill 绑定响应 DTO（Workspace 维度）。
 *
 * @since 1.0
 */
@Data
@Builder
public class SkillBindingResponse {

    private Long id;
    private Long workspaceId;       // null 表示全局绑定;
    private String characterId;     // null 表示全局绑定;

    /** Skill 基础信息 */
    private Long skillId;
    private String skillName;
    private String skillDescription;
    private String category;

    /** 版本策略 */
    private Integer pinnedVersion;
    private Boolean useLatest;

    /** 当前 skill 的最新版本（用于提示用户是否有新版本可用） */
    private Integer latestVersion;

    /** 是否有新版本可用 */
    private Boolean hasNewVersion;

    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

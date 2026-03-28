package org.dragon.skill.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Character 绑定的 Skill 响应 DTO。
 *
 * @since 1.0
 */
@Data
@Builder
public class CharacterSkillResponse {

    private Long id;
    private String characterId;
    private Long workspaceId;      // null 表示全局绑定

    /** Skill 基础信息 */
    private Long skillId;
    private String skillName;
    private String skillDescription;
    private String category;

    /** 版本策略 */
    private Integer pinnedVersion;
    private Boolean useLatest;

    /** 当前 skill 的最新版本 */
    private Integer latestVersion;

    /** 是否有新版本可用 */
    private Boolean hasNewVersion;

    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
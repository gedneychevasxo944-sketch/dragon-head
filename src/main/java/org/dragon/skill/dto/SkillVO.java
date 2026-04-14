package org.dragon.skill.dto;

import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.SkillVisibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 详情视图对象。
 *
 * <p>用于详情页接口（{@code GET /api/v1/skills/{skillId}}）。
 * 包含技能元信息、版本信息、SKILL.md 正文，以及引用该 Skill 的 Character 和 Workspace 简要信息。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillVO {

    // ── 技能元信息 ───────────────────────────────────────────────

    /** 技能 ID */
    private String id;

    private String name;

    private String displayName;

    private String introduction;

    private String description;

    private SkillCategory category;

    private SkillVisibility visibility;

    /** 标签列表 */
    private List<String> tags;

    private SkillStatus status;

    private StorageInfo storageInfo;

    // ── 版本信息 ─────────────────────────────────────────────────

    /** 当前已发布版本号 */
    private Integer version;

    /** 版本状态 */
    private SkillVersionStatus versionStatus;

    // ── 创建/编辑信息 ────────────────────────────────────────────

    private Long creatorId;
    private String creatorName;
    private LocalDateTime createdAt;

    private Long editorId;
    private String editorName;
    private LocalDateTime publishedAt;

    // ── 关联引用 ─────────────────────────────────────────────────

    /** 引用该 Skill 的 Character 列表 */
    private List<CharacterRef> characters;

    /** 引用该 Skill 的 Workspace 列表 */
    private List<WorkspaceRef> workspaces;


    // ── 内嵌引用类型 ─────────────────────────────────────────────

    /**
     * Character 简要信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CharacterRef {
        private String id;
        private String name;
        private String avatar;
    }

    /**
     * Workspace 简要信息。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceRef {
        private String id;
        private String name;
    }
}

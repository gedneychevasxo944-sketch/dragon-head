package org.dragon.skill.dto;

import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 绑定 Skill 到 Character / Workspace / Character+Workspace 的请求体。
 *
 * <p>三种绑定场景均使用此 DTO，由调用方（WorkspaceService / CharacterService）
 * 在传入前填好 {@code bindingType}、{@code characterId}、{@code workspaceId}，
 * HTTP 层只需用户提供 skillId 和版本策略相关字段。
 *
 * <p>字段填写规则：
 * <pre>
 * bindingType         | characterId | workspaceId
 * --------------------+-------------+-------------
 * character           | 必填        | null
 * workspace           | null        | 必填
 * character_workspace | 必填        | 必填
 * </pre>
 *
 * <p>版本策略：
 * <ul>
 *   <li>{@code versionType = "latest"}（默认）：绑定最新已发布版本，fixedVersion 忽略</li>
 *   <li>{@code versionType = "fixed"}：绑定到 fixedVersion 指定的具体版本号</li>
 * </ul>
 */
@Data
public class SkillBindingRequest {

    // ── 由调用方（Service 层）在传入前填充，HTTP 层不暴露 ──────────────

    /**
     * 绑定类型（由 Service 层填入，不来自 HTTP 请求体）。
     * 取值：character / workspace / character_workspace
     */
    private String bindingType;

    /**
     * Character 主键（由 Service 层填入）。
     * bindingType = 'character' 或 'character_workspace' 时必填。
     */
    private String characterId;

    /**
     * Workspace 主键（由 Service 层填入）。
     * bindingType = 'workspace' 或 'character_workspace' 时必填。
     */
    private String workspaceId;

    // ── HTTP 请求体字段 ────────────────────────────────────────────────

    /** 技能唯一标识（UUID） */
    @NotBlank(message = "skillId 不能为空")
    private String skillId;

    /**
     * 版本类型。
     * <ul>
     *   <li>{@code latest}（默认）：运行时取最新 active 版本</li>
     *   <li>{@code fixed}：固定到 fixedVersion 指定版本</li>
     * </ul>
     */
    @Pattern(regexp = "latest|fixed", message = "versionType 只允许 latest 或 fixed")
    private String versionType = "latest";

    /**
     * 固定版本号（对应 skills.version）。
     * 仅 versionType = 'fixed' 时必填，latest 时无需传入。
     */
    @Min(value = 1, message = "fixedVersion 最小为 1")
    private Integer fixedVersion;
}


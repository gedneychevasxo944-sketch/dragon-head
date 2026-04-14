package org.dragon.tool.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.tool.enums.ToolCreatorType;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVisibility;

import java.time.LocalDateTime;

/**
 * 工具主记录（Domain Object）。
 *
 * <p>描述一个工具的元信息和已发布版本指针。工具的具体声明（description/parameters）
 * 和执行配置存放在 {@link ToolVersionDO} 中，通过 {@link #publishedVersionId} 关联。
 *
 * <p><b>可见性规则</b>（{@link ToolVisibility}，描述"谁能看到/使用"）：
 * <ul>
 *   <li>{@link ToolVisibility#PUBLIC}    - 发布到工具广场，任何人可搜索并绑定</li>
 *   <li>{@link ToolVisibility#WORKSPACE} - 需绑定到 workspace 后可用</li>
 *   <li>{@link ToolVisibility#PRIVATE}   - 需绑定到 character 后才可用</li>
 * </ul>
 *
 * <p><b>内置规则</b>（{@link #builtin}，描述"是否默认加载"）：
 * {@link #builtin} = true 的工具由平台预置，对所有 agent 默认可用，无需手动绑定。
 * 与 {@link #visibility} 正交：内置工具通常同时设置 visibility=PUBLIC（可被广场发现），
 * 但两者语义相互独立——visibility 控制"谁能看到"，builtin 控制"是否默认加载"。
 *
 * <p><b>命名约定</b>：
 * <ul>
 *   <li>ATOMIC 工具：如 {@code bash}、{@code file_read}</li>
 *   <li>MCP 工具：格式为 {@code mcp__{serverName}__{toolName}}，如 {@code mcp__github__search_code}</li>
 *   <li>HTTP/CODE/SKILL 工具：自定义名称，需全局唯一</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDO {

    // ── 标识 ─────────────────────────────────────────────────────

    /** 工具唯一 ID */
    private String id;

    // ── 基本元信息 ────────────────────────────────────────────────

    /**
     * 工具名称，LLM 通过此名称发起 tool_call。
     * MCP 工具名格式为 {@code mcp__{serverName}__{toolName}}。
     */
    private String name;

    /** 工具展示名称（页面展示用）。 */
    private String displayName;

    /**
     * 工具简介（管理页面展示用）。
     * 工具的具体描述（用于 LLM 理解工具用途）存放在 {@link ToolVersionDO#getDeclaration()} 中。
     */
    private String introduction;

    /** 工具类型 */
    private ToolType toolType;

    /**
     * 可见性。
     * <ul>
     *   <li>PUBLIC    - 发布到工具广场，所有人可见、可绑定</li>
     *   <li>WORKSPACE - 需绑定到 workspace 后可用</li>
     *   <li>PRIVATE   - 需绑定到 character 后才可用</li>
     * </ul>
     */
    @Builder.Default
    private ToolVisibility visibility = ToolVisibility.PRIVATE;

    /**
     * 是否为系统内置工具。
     *
     * <p>true 时工具由平台预置，对所有 agent 默认可用，无需手动绑定。
     * 与 {@link #visibility} 正交：内置工具可以同时拥有任意 visibility，两者语义独立。
     */
    @Builder.Default
    private boolean builtin = false;

    /** 标签列表（JSON 数组字符串，如 ["搜索","文件"]，用于广场筛选展示） */
    private String tags;

    // ── 创建者 ────────────────────────────────────────────────────

    /** 创建者类型（PERSONAL / OFFICIAL） */
    private ToolCreatorType creatorType;

    /** 创建者用户 ID */
    private Long creatorId;

    /** 创建者用户名 */
    private String creatorName;

    // ── 状态与版本指针 ────────────────────────────────────────────

    /**
     * 工具状态。ToolRegistry 只返回 status = {@link ToolStatus#ACTIVE} 的工具。
     */
    @Builder.Default
    private ToolStatus status = ToolStatus.ACTIVE;

    /**
     * 已发布版本的 ID，指向 {@link ToolVersionDO}。
     * LLM 调用默认使用此版本，暂不支持指定版本调用。
     */
    private Long publishedVersionId;

    // ── 时间戳 ────────────────────────────────────────────────────

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 删除时间（软删除标记，非 null 时表示已删除） */
    private LocalDateTime deletedAt;

    // ── 语义方法 ──────────────────────────────────────────────────

    /** 是否已发布（publishedVersionId 不为 null） */
    public boolean isPublished() {
        return publishedVersionId != null;
    }

    /** 是否已被软删除 */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** 是否全局可见（visibility = PUBLIC） */
    public boolean isPublic() {
        return visibility == ToolVisibility.PUBLIC;
    }
}

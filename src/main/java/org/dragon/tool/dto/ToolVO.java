package org.dragon.tool.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVersionStatus;
import org.dragon.tool.enums.ToolVisibility;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tool 详情视图对象。
 *
 * <p>用于详情页接口（{@code GET /api/v1/tools/{id}}）及列表接口。
 * 包含工具元信息、版本信息，以及引用该 Tool 的 Character 和 Workspace 简要信息。
 *
 * @author ypf
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolVO {

    // ── 工具元信息 ───────────────────────────────────────────────

    /** 工具 ID */
    private String id;

    /** 工具调用名称（LLM tool_call 使用） */
    private String name;

    /** 工具展示名称（页面展示用） */
    private String displayName;

    /** 工具简介 */
    private String introduction;

    /** 工具描述（LLM 理解工具用途的描述） */
    private String description;

    /** 工具类型 */
    private ToolType toolType;

    /** 可见性 */
    private ToolVisibility visibility;

    /** 是否为系统内置工具 */
    private Boolean builtin;

    /** 标签列表 */
    private List<String> tags;

    /** 工具状态 */
    private ToolStatus status;

    // ── 版本信息 ─────────────────────────────────────────────────

    /** 当前已发布版本号 */
    private Integer version;

    /** 版本状态 */
    private ToolVersionStatus versionStatus;

    // ── 创建/编辑信息 ────────────────────────────────────────────

    private Long creatorId;
    private String creatorName;
    private LocalDateTime createdAt;

    private Long editorId;
    private String editorName;
    private LocalDateTime publishedAt;

    // ── 关联引用 ─────────────────────────────────────────────────

    /** 引用该 Tool 的 Character 列表 */
    private List<CharacterRef> characters;

    /** 引用该 Tool 的 Workspace 列表 */
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

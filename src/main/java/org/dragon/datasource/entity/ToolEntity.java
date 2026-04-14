package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.enums.ToolCreatorType;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVisibility;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ToolEntity — 映射 tool 表（工具主表，元信息）。
 *
 * <p>与 ToolVersionEntity 的关系：
 * <ul>
 *   <li>一个 ToolEntity 对应多个 ToolVersionEntity（1:N）</li>
 *   <li>publishedVersionId 指向当前已发布的版本</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool")
public class ToolEntity {

    @Id
    private String id;

    // ── 基本元信息 ───────────────────────────────────────────────────

    /** 工具名称，LLM 通过此名称发起 tool_call */
    @Column(nullable = false, length = 128)
    private String name;

    /** 工具展示名称（页面展示用） */
    @Column(name = "display_name", length = 128)
    private String displayName;

    /** 工具简介（管理页面展示用） */
    @Column(columnDefinition = "TEXT")
    private String introduction;

    /** 工具类型 */
    @Column(name = "tool_type", nullable = false, length = 32)
    private ToolType toolType;

    /** 可见性：PUBLIC / WORKSPACE / PRIVATE */
    @Column(nullable = false, length = 20)
    private ToolVisibility visibility;

    /** 是否为系统内置工具 */
    @Column(nullable = false)
    private boolean builtin;

    /** 标签列表（JSON 数组） */
    @Column(columnDefinition = "JSON")
    @DbJson
    private List<String> tags;

    // ── 创建者 ───────────────────────────────────────────────────────

    /** 创建者类型 */
    @Column(name = "creator_type", length = 20)
    private ToolCreatorType creatorType;

    /** 创建者用户 ID */
    @Column(name = "creator_id")
    private Long creatorId;

    /** 创建者用户名 */
    @Column(name = "creator_name", length = 100)
    private String creatorName;

    // ── 状态与版本指针 ───────────────────────────────────────────────

    /** 工具状态 */
    @Column(nullable = false, length = 20)
    private ToolStatus status;

    /** 已发布版本 ID（指向 tool_versions.id） */
    @Column(name = "published_version_id")
    private Long publishedVersionId;

    // ── 时间戳 ──────────────────────────────────────────────────────

    /** 创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /** 删除时间（软删除标记） */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── 转换方法 ─────────────────────────────────────────────────────

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 转换为 ToolDO。
     */
    public ToolDO toDomain() {
        return ToolDO.builder()
                .id(this.id)
                .name(this.name)
                .displayName(this.displayName)
                .introduction(this.introduction)
                .toolType(this.toolType)
                .visibility(this.visibility != null ? this.visibility : ToolVisibility.PRIVATE)
                .builtin(this.builtin)
                .tags(toJsonString(this.tags))
                .creatorType(this.creatorType)
                .creatorId(this.creatorId)
                .creatorName(this.creatorName)
                .status(this.status != null ? this.status : ToolStatus.ACTIVE)
                .publishedVersionId(this.publishedVersionId)
                .createdAt(this.createdAt)
                .deletedAt(this.deletedAt)
                .build();
    }

    /**
     * 从 ToolDO 创建 Entity。
     */
    public static ToolEntity fromDomain(ToolDO domain) {
        return ToolEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .displayName(domain.getDisplayName())
                .introduction(domain.getIntroduction())
                .toolType(domain.getToolType())
                .visibility(domain.getVisibility())
                .builtin(domain.isBuiltin())
                .tags(fromJsonString(domain.getTags()))
                .creatorType(domain.getCreatorType())
                .creatorId(domain.getCreatorId())
                .creatorName(domain.getCreatorName())
                .status(domain.getStatus())
                .publishedVersionId(domain.getPublishedVersionId())
                .createdAt(domain.getCreatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    private static String toJsonString(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> fromJsonString(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return null;
        }
    }
}
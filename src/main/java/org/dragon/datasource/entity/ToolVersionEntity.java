package org.dragon.datasource.entity;

import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVersionStatus;

import java.time.LocalDateTime;

/**
 * ToolVersionEntity — 映射 tool_version 表（工具版本）。
 *
 * <p>每次更新工具内容时 INSERT 新记录，toolId 不变，version +1。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_version")
public class ToolVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 归属 ──────────────────────────────────────────────────────────

    /** 所属工具 ID（关联 tool.id） */
    @Column(name = "tool_id", nullable = false, length = 64)
    private String toolId;

    /** 版本号（从 1 开始） */
    @Column(nullable = false)
    private Integer version;

    // ── LLM 声明字段 ──────────────────────────────────────────────────

    /** LLM tool_call 使用的工具名称 */
    @Column(nullable = false, length = 128)
    private String name;

    /** 工具描述（注入 LLM system prompt） */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 参数 Schema（JSON 格式，Map&lt;String, ParameterSchema&gt;） */
    @Column(columnDefinition = "JSON")
    private String parameters;

    /** 必填参数名列表（JSON 数组，如 ["command","path"]） */
    @Column(name = "required_params", columnDefinition = "JSON")
    private String requiredParams;

    /** 工具别名列表（JSON 数组，可为 null） */
    @Column(columnDefinition = "JSON")
    private String aliases;

    // ── 执行配置 ───────────────────────────────────────────────────────

    /** 执行配置（JSON 格式，结构因 tool_type 不同而异） */
    @Column(name = "execution_config", columnDefinition = "JSON")
    private String executionConfig;

    // ── 冗余字段 ───────────────────────────────────────────────────────

    /** 工具类型（冗余字段，避免 JOIN tool 表） */
    @Column(name = "tool_type", nullable = false, length = 32)
    private ToolType toolType;

    // ── 编辑者 ────────────────────────────────────────────────────────

    /** 本次编辑者用户 ID */
    @Column(name = "editor_id")
    private Long editorId;

    /** 本次编辑者用户名 */
    @Column(name = "editor_name", length = 100)
    private String editorName;

    // ── 存储信息 ───────────────────────────────────────────────────────

    /** 文件存储元信息（ToolStorageInfoVO 的 JSON 序列化，type 内聚在 JSON 中） */
    @Column(name = "storage_info", columnDefinition = "JSON")
    private String storageInfo;

    // ── 版本状态 ───────────────────────────────────────────────────────

    /** 版本状态 */
    @Column(nullable = false, length = 20)
    private ToolVersionStatus status;

    /** 发版备注 */
    @Column(name = "release_note", columnDefinition = "TEXT")
    private String releaseNote;

    // ── 时间戳 ────────────────────────────────────────────────────────

    /** 版本创建时间 */
    @Column(name = "created_at")
    @WhenCreated
    private LocalDateTime createdAt;

    /** 发布时间 */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ── 转换方法 ──────────────────────────────────────────────────────

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 转换为 ToolVersionDO。
     */
    public ToolVersionDO toDomain() {
        ToolVersionDO domain = new ToolVersionDO();
        domain.setId(this.id);
        domain.setToolId(this.toolId);
        domain.setVersion(this.version);
        domain.setName(this.name);
        domain.setDescription(this.description);
        domain.setParameters(this.parameters);
        domain.setRequiredParams(this.requiredParams);
        domain.setAliases(this.aliases);
        domain.setExecutionConfig(parseJson(this.executionConfig));
        domain.setToolType(this.toolType);
        domain.setEditorId(this.editorId);
        domain.setEditorName(this.editorName);
        domain.setStorageInfo(this.storageInfo);
        domain.setStatus(this.status);
        domain.setReleaseNote(this.releaseNote);
        domain.setCreatedAt(this.createdAt);
        domain.setPublishedAt(this.publishedAt);
        return domain;
    }

    /**
     * 从 ToolVersionDO 创建 Entity。
     */
    public static ToolVersionEntity fromDomain(ToolVersionDO domain) {
        return ToolVersionEntity.builder()
                .id(domain.getId())
                .toolId(domain.getToolId())
                .version(domain.getVersion())
                .name(domain.getName())
                .description(domain.getDescription())
                .parameters(domain.getParameters())
                .requiredParams(domain.getRequiredParams())
                .aliases(domain.getAliases())
                .executionConfig(toJsonString(domain.getExecutionConfig()))
                .toolType(domain.getToolType())
                .editorId(domain.getEditorId())
                .editorName(domain.getEditorName())
                .storageInfo(domain.getStorageInfo())
                .status(domain.getStatus())
                .releaseNote(domain.getReleaseNote())
                .createdAt(domain.getCreatedAt())
                .publishedAt(domain.getPublishedAt())
                .build();
    }

    private static com.fasterxml.jackson.databind.JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}


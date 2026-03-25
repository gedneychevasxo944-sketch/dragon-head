package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import org.dragon.workspace.commons.CommonSense;
import java.util.Map;

/**
 * CommonSenseEntity 常识实体
 * 映射数据库 common_sense 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "common_sense")
public class CommonSenseEntity {

    @Id
    private String id;

    private String workspaceId;

    private String folderId;

    private String name;

    private String description;

    private String category;

    private String rule;

    private String severity;

    private int version;

    private boolean enabled;

    private String promptTemplate;

    @DbJson
    private Map<String, Object> promptVariables;

    private String content;

    private String cachedPrompt;

    private LocalDateTime lastPromptUpdateAt;

    private String promptUpdateSource;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    /**
     * 转换为CommonSense
     */
    public CommonSense toCommonSense() {
        return CommonSense.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .folderId(this.folderId)
                .name(this.name)
                .description(this.description)
                .category(this.category != null ? CommonSense.Category.valueOf(this.category) : null)
                .rule(this.rule)
                .severity(this.severity != null ? CommonSense.Severity.valueOf(this.severity) : null)
                .version(this.version)
                .enabled(this.enabled)
                .promptTemplate(this.promptTemplate)
                .promptVariables(this.promptVariables)
                .content(this.content)
                .cachedPrompt(this.cachedPrompt)
                .lastPromptUpdateAt(this.lastPromptUpdateAt)
                .promptUpdateSource(this.promptUpdateSource != null ? CommonSense.UpdateSource.valueOf(this.promptUpdateSource) : null)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .createdBy(this.createdBy)
                .build();
    }

    /**
     * 从CommonSense创建Entity
     */
    public static CommonSenseEntity fromCommonSense(CommonSense commonSense) {
        return CommonSenseEntity.builder()
                .id(commonSense.getId())
                .workspaceId(commonSense.getWorkspaceId())
                .folderId(commonSense.getFolderId())
                .name(commonSense.getName())
                .description(commonSense.getDescription())
                .category(commonSense.getCategory() != null ? commonSense.getCategory().name() : null)
                .rule(commonSense.getRule())
                .severity(commonSense.getSeverity() != null ? commonSense.getSeverity().name() : null)
                .version(commonSense.getVersion())
                .enabled(commonSense.isEnabled())
                .promptTemplate(commonSense.getPromptTemplate())
                .promptVariables(commonSense.getPromptVariables())
                .content(commonSense.getContent())
                .cachedPrompt(commonSense.getCachedPrompt())
                .lastPromptUpdateAt(commonSense.getLastPromptUpdateAt())
                .promptUpdateSource(commonSense.getPromptUpdateSource() != null ? commonSense.getPromptUpdateSource().name() : null)
                .createdAt(commonSense.getCreatedAt())
                .updatedAt(commonSense.getUpdatedAt())
                .createdBy(commonSense.getCreatedBy())
                .build();
    }
}

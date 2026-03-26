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

import org.dragon.workspace.material.ParsedMaterialContent;
import java.util.Map;

/**
 * ParsedMaterialContentEntity 解析后物料内容实体
 * 映射数据库 parsed_material_content 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "parsed_material_content")
public class ParsedMaterialContentEntity {

    @Id
    private String id;

    private String materialId;

    private String textContent;

    @DbJson
    private Object structuredContent;

    @DbJson
    private Map<String, Object> metadata;

    private String status;

    private String errorMessage;

    private LocalDateTime parsedAt;

    /**
     * AI 生成的摘要
     */
    private String summary;

    /**
     * 转换为ParsedMaterialContent
     */
    public ParsedMaterialContent toParsedMaterialContent() {
        return ParsedMaterialContent.builder()
                .id(this.id)
                .materialId(this.materialId)
                .textContent(this.textContent)
                .structuredContent(this.structuredContent)
                .metadata(this.metadata)
                .status(this.status != null ? ParsedMaterialContent.ParseStatus.valueOf(this.status) : null)
                .errorMessage(this.errorMessage)
                .parsedAt(this.parsedAt)
                .summary(this.summary)
                .build();
    }

    /**
     * 从ParsedMaterialContent创建Entity
     */
    public static ParsedMaterialContentEntity fromParsedMaterialContent(ParsedMaterialContent content) {
        return ParsedMaterialContentEntity.builder()
                .id(content.getId())
                .materialId(content.getMaterialId())
                .textContent(content.getTextContent())
                .structuredContent(content.getStructuredContent())
                .metadata(content.getMetadata())
                .status(content.getStatus() != null ? content.getStatus().name() : null)
                .errorMessage(content.getErrorMessage())
                .parsedAt(content.getParsedAt())
                .summary(content.getSummary())
                .build();
    }
}

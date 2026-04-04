package org.dragon.datasource.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import org.dragon.tools.AgentTool;

/**
 * ToolEntity 工具实体
 * 映射数据库 tool 表
 * 注意：只存储工具元数据，执行逻辑由代码实现
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool")
public class ToolEntity {

    @Id
    private String name;

    private String description;

    @Column(name = "parameter_schema", columnDefinition = "TEXT")
    private String parameterSchema;

    private Boolean enabled;

    /**
     * 转换为工具元数据（用于重建AgentTool实例）
     * 返回一个包含元数据的Map，实际AgentTool由ToolRegistry管理
     */
    public Map<String, Object> toToolMetadata() {
        return Map.of(
                "name", this.name,
                "description", this.description != null ? this.description : "",
                "enabled", this.enabled != null ? this.enabled : true
        );
    }

    /**
     * 从AgentTool创建Entity（提取元数据）
     */
    public static ToolEntity fromAgentTool(AgentTool tool) {
        String schemaJson = null;
        if (tool.getParameterSchema() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                schemaJson = mapper.writeValueAsString(tool.getParameterSchema());
            } catch (Exception e) {
                schemaJson = "{}";
            }
        }

        return ToolEntity.builder()
                .name(tool.getName())
                .description(tool.getDescription())
                .parameterSchema(schemaJson)
                .enabled(true) // 默认启用
                .build();
    }
}
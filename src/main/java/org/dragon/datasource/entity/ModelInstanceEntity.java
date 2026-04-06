package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import org.dragon.agent.model.ModelInstance;

/**
 * ModelInstanceEntity 模型实例实体
 * 映射数据库 model_instance 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model_instance")
public class ModelInstanceEntity {

    @Id
    private String id;

    private String provider;

    @Column(name = "model_name")
    private String modelName;

    private String endpoint;

    @DbJson
    private Map<String, String> credentials;

    @Column(name = "default_params")
    @DbJson
    private Map<String, Object> defaultParams;

    private Boolean enabled;

    private String description;

    private Integer priority;

    /**
     * 转换为ModelInstance
     */
    public ModelInstance toModelInstance() {
        return ModelInstance.builder()
                .id(this.id)
                .provider(this.provider != null ? ModelInstance.ModelProvider.valueOf(this.provider) : null)
                .modelName(this.modelName)
                .endpoint(this.endpoint)
                .credentials(this.credentials)
                .defaultParams(this.defaultParams)
                .enabled(this.enabled != null ? this.enabled : false)
                .description(this.description)
                .priority(this.priority != null ? this.priority : 0)
                .build();
    }

    /**
     * 从ModelInstance创建Entity
     */
    public static ModelInstanceEntity fromModelInstance(ModelInstance modelInstance) {
        return ModelInstanceEntity.builder()
                .id(modelInstance.getId())
                .provider(modelInstance.getProvider() != null ? modelInstance.getProvider().name() : null)
                .modelName(modelInstance.getModelName())
                .endpoint(modelInstance.getEndpoint())
                .credentials(modelInstance.getCredentials())
                .defaultParams(modelInstance.getDefaultParams())
                .enabled(modelInstance.isEnabled())
                .description(modelInstance.getDescription())
                .priority(modelInstance.getPriority())
                .build();
    }
}
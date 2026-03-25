package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import org.dragon.agent.workflow.Workflow;
import org.dragon.agent.workflow.TerminationConfig;

/**
 * WorkflowEntity 工作流实体
 * 映射数据库 workflow 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workflow")
public class WorkflowEntity {

    @Id
    private String id;

    private String name;

    @DbJson
    private List<Workflow.Node> nodes;

    @DbJson
    private Map<String, Object> variables;

    @DbJson
    private TerminationConfig terminationConfig;

    private String description;

    /**
     * 转换为Workflow
     */
    public Workflow toWorkflow() {
        return Workflow.builder()
                .id(this.id)
                .name(this.name)
                .nodes(this.nodes)
                .variables(this.variables)
                .terminationConfig(this.terminationConfig)
                .description(this.description)
                .build();
    }

    /**
     * 从Workflow创建Entity
     */
    public static WorkflowEntity fromWorkflow(Workflow workflow) {
        return WorkflowEntity.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .nodes(workflow.getNodes())
                .variables(workflow.getVariables())
                .terminationConfig(workflow.getTerminationConfig())
                .description(workflow.getDescription())
                .build();
    }
}

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dragon.agent.workflow.WorkflowState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkflowStateEntity 工作流执行状态实体
 * 映射数据库 workflow_state 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workflow_state")
public class WorkflowStateEntity {

    @Id
    private String executionId;

    private String workflowId;

    private String characterId;

    private String currentNodeId;

    @DbJson
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    @DbJson
    @Builder.Default
    private Map<String, Object> results = new HashMap<>();

    private String status;

    private int currentStep;

    private int loopIteration;

    @DbJson
    @Builder.Default
    private List<String> errorMessages = new ArrayList<>();

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * 转换为WorkflowState
     */
    public WorkflowState toWorkflowState() {
        return WorkflowState.builder()
                .executionId(this.executionId)
                .workflowId(this.workflowId)
                .characterId(this.characterId)
                .currentNodeId(this.currentNodeId)
                .context(this.context)
                .results(this.results)
                .status(this.status != null ? WorkflowState.State.valueOf(this.status) : null)
                .currentStep(this.currentStep)
                .loopIteration(this.loopIteration)
                .errorMessages(this.errorMessages)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .build();
    }

    /**
     * 从WorkflowState创建Entity
     */
    public static WorkflowStateEntity fromWorkflowState(WorkflowState state) {
        return WorkflowStateEntity.builder()
                .executionId(state.getExecutionId())
                .workflowId(state.getWorkflowId())
                .characterId(state.getCharacterId())
                .currentNodeId(state.getCurrentNodeId())
                .context(state.getContext())
                .results(state.getResults())
                .status(state.getStatus() != null ? state.getStatus().name() : null)
                .currentStep(state.getCurrentStep())
                .loopIteration(state.getLoopIteration())
                .errorMessages(state.getErrorMessages())
                .startTime(state.getStartTime())
                .endTime(state.getEndTime())
                .build();
    }
}

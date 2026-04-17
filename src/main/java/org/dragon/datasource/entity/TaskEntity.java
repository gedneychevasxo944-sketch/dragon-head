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
import java.util.List;
import java.util.Map;

import org.dragon.task.Task;
import org.dragon.task.TaskStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TaskEntity 任务实体
 * 映射数据库 task 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "task")
public class TaskEntity {

    @Id
    private String id;

    private String workspaceId;

    private String parentTaskId;

    private String creatorId;

    private String characterId;

    private String name;

    private String description;

    private String status;

    @DbJson
    private Object input;

    @DbJson
    private Object output;

    private String result;

    private String errorMessage;

    @DbJson
    @Builder.Default
    private List<String> childTaskIds = new ArrayList<>();

    @DbJson
    @Builder.Default
    private List<String> assignedMemberIds = new ArrayList<>();

    @DbJson
    @Builder.Default
    private List<Task.ExecutionStep> executionSteps = new ArrayList<>();

    @DbJson
    @Builder.Default
    private List<Task.ExecutionMessage> executionMessages = new ArrayList<>();

    private String currentStreamingContent;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private String executionMode;

    private String workflowId;

    @DbJson
    @Builder.Default
    private List<String> dependencyTaskIds = new ArrayList<>();

    private String waitingReason;

    private String originalCharacterId;

    @DbJson
    @Builder.Default
    private List<String> claimerIds = new ArrayList<>();

    private String waitingForCharacterId;

    private String resumeToken;

    @DbJson
    private Map<String, Object> resumeContext;

    private String sourceMessageId;

    private String sourceChatId;

    private String sourceChannel;

    @DbJson
    private List<String> materialIds;

    private String lastQuestion;

    @DbJson
    private Object interactionContext;

    @DbJson
    private Map<String, Object> metadata;

    @DbJson
    private Map<String, Object> extensions;

    /**
     * 转换为Task
     */
    public Task toTask() {
        return Task.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .parentTaskId(this.parentTaskId)
                .creatorId(this.creatorId)
                .characterId(this.characterId)
                .name(this.name)
                .description(this.description)
                .status(this.status != null ? TaskStatus.valueOf(this.status) : null)
                .input(this.input)
                .output(this.output)
                .result(this.result)
                .errorMessage(this.errorMessage)
                .childTaskIds(this.childTaskIds)
                .assignedMemberIds(this.assignedMemberIds)
                .executionSteps(this.executionSteps)
                .executionMessages(this.executionMessages)
                .currentStreamingContent(this.currentStreamingContent)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .startedAt(this.startedAt)
                .completedAt(this.completedAt)
                .executionMode(this.executionMode)
                .workflowId(this.workflowId)
                .dependencyTaskIds(this.dependencyTaskIds)
                .waitingReason(this.waitingReason)
                .originalCharacterId(this.originalCharacterId)
                .claimerIds(this.claimerIds)
                .waitingForCharacterId(this.waitingForCharacterId)
                .resumeToken(this.resumeToken)
                .resumeContext(this.resumeContext)
                .sourceMessageId(this.sourceMessageId)
                .sourceChatId(this.sourceChatId)
                .sourceChannel(this.sourceChannel)
                .materialIds(this.materialIds)
                .lastQuestion(this.lastQuestion)
                .interactionContext(this.interactionContext)
                .metadata(this.metadata)
                .extensions(this.extensions)
                .build();
    }

    /**
     * 从Task创建Entity
     */
    public static TaskEntity fromTask(Task task) {
        return TaskEntity.builder()
                .id(task.getId())
                .workspaceId(task.getWorkspaceId())
                .parentTaskId(task.getParentTaskId())
                .creatorId(task.getCreatorId())
                .characterId(task.getCharacterId())
                .name(task.getName())
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().name() : null)
                .input(task.getInput())
                .output(task.getOutput())
                .result(task.getResult())
                .errorMessage(task.getErrorMessage())
                .childTaskIds(task.getChildTaskIds())
                .assignedMemberIds(task.getAssignedMemberIds())
                .executionSteps(task.getExecutionSteps())
                .executionMessages(task.getExecutionMessages())
                .currentStreamingContent(task.getCurrentStreamingContent())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .executionMode(task.getExecutionMode())
                .workflowId(task.getWorkflowId())
                .dependencyTaskIds(task.getDependencyTaskIds())
                .waitingReason(task.getWaitingReason())
                .originalCharacterId(task.getOriginalCharacterId())
                .claimerIds(task.getClaimerIds())
                .waitingForCharacterId(task.getWaitingForCharacterId())
                .resumeToken(task.getResumeToken())
                .resumeContext(task.getResumeContext())
                .sourceMessageId(task.getSourceMessageId())
                .sourceChatId(task.getSourceChatId())
                .sourceChannel(task.getSourceChannel())
                .materialIds(task.getMaterialIds())
                .lastQuestion(task.getLastQuestion())
                .interactionContext(task.getInteractionContext())
                .metadata(task.getMetadata())
                .extensions(task.getExtensions())
                .build();
    }
}

package org.dragon.datasource.entity;

import io.ebean.annotation.DbJson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.workspace.chat.ChatSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ChatSessionEntity 聊天会话实体
 * 映射数据库 chat_session 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_session", indexes = {
    @Index(name = "idx_session_workspace_id", columnList = "workspace_id"),
    @Index(name = "idx_session_task_id", columnList = "task_id"),
    @Index(name = "idx_session_status", columnList = "status")
})
public class ChatSessionEntity {

    @Id
    private String id;

    private String workspaceId;

    private String taskId;

    @DbJson
    private List<String> participantIds;

    @DbJson
    private Map<String, Object> context;

    @DbJson
    private Map<String, String> participantStates;

    @DbJson
    private Map<String, String> taskStates;

    @DbJson
    private List<String> blockedParticipants;

    private String lastSummary;

    @DbJson
    private List<DecisionRecordEntity> decisions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String status;

    /**
     * DecisionRecord 决策记录实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionRecordEntity {
        private String id;
        private String characterId;
        private String decision;
        private String rationale;
        private LocalDateTime timestamp;
    }

    /**
     * 转换为ChatSession
     */
    public ChatSession toChatSession() {
        return ChatSession.builder()
                .id(this.id)
                .workspaceId(this.workspaceId)
                .taskId(this.taskId)
                .participantIds(this.participantIds)
                .context(this.context)
                .participantStates(this.participantStates)
                .taskStates(this.taskStates)
                .blockedParticipants(this.blockedParticipants)
                .lastSummary(this.lastSummary)
                .decisions(this.decisions != null ? this.decisions.stream()
                        .map(this::toDecisionRecord)
                        .toList() : null)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .status(this.status != null ? ChatSession.Status.valueOf(this.status) : null)
                .build();
    }

    private ChatSession.DecisionRecord toDecisionRecord(DecisionRecordEntity entity) {
        return ChatSession.DecisionRecord.builder()
                .id(entity.getId())
                .characterId(entity.getCharacterId())
                .decision(entity.getDecision())
                .rationale(entity.getRationale())
                .timestamp(entity.getTimestamp())
                .build();
    }

    /**
     * 从ChatSession创建Entity
     */
    public static ChatSessionEntity fromChatSession(ChatSession session) {
        return ChatSessionEntity.builder()
                .id(session.getId())
                .workspaceId(session.getWorkspaceId())
                .taskId(session.getTaskId())
                .participantIds(session.getParticipantIds())
                .context(session.getContext())
                .participantStates(session.getParticipantStates())
                .taskStates(session.getTaskStates())
                .blockedParticipants(session.getBlockedParticipants())
                .lastSummary(session.getLastSummary())
                .decisions(session.getDecisions() != null ? session.getDecisions().stream()
                        .map(ChatSessionEntity::toDecisionRecordEntity)
                        .toList() : null)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .status(session.getStatus() != null ? session.getStatus().name() : null)
                .build();
    }

    private static DecisionRecordEntity toDecisionRecordEntity(ChatSession.DecisionRecord record) {
        return DecisionRecordEntity.builder()
                .id(record.getId())
                .characterId(record.getCharacterId())
                .decision(record.getDecision())
                .rationale(record.getRationale())
                .timestamp(record.getTimestamp())
                .build();
    }
}

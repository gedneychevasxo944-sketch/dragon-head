package org.dragon.workspace.task;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TaskBridgeContext - Character 执行上下文（保留用于兼容性）
 *
 * <p>已被 Step 执行框架取代，此处仅保留用于兼容现有代码。
 *
 * @author wyj
 * @deprecated Use Step + TaskContext instead
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class TaskBridgeContext {
    private String workspaceId;
    private String parentTaskId;
    private String collaborationSessionId;
    private List<String> peerCharacterIds;
    private List<String> dependencyTaskIds;
    private List<String> latestSessionMessages;
    private boolean collaborationJudgementEnabled;
    private Map<String, Object> extra;

    // Collaboration session state
    private Map<String, String> participantStates;
    private List<String> blockedParticipants;
    private String sessionStatus;
    private String materialContext;
}
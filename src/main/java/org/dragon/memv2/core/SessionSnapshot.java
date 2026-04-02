package org.dragon.memv2.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话快照类
 * 表示当前会话的抽象摘要，包含会话状态、当前目标、最近决策等信息
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSnapshot {
    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 角色ID
     */
    private String characterId;

    /**
     * 工作空间ID
     */
    private String workspaceId;

    /**
     * 会话摘要
     */
    private String summary;

    /**
     * 当前目标
     */
    private String currentGoal;

    /**
     * 最近决策列表
     */
    @Builder.Default
    private List<String> recentDecisions = new ArrayList<>();

    /**
     * 未解决问题列表
     */
    @Builder.Default
    private List<String> unresolvedQuestions = new ArrayList<>();

    /**
     * 会话内容
     */
    private String content;

    /**
     * 更新时间
     */
    private Instant updatedAt;
}

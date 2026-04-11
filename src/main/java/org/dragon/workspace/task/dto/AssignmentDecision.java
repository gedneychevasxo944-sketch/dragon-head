package org.dragon.workspace.task.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分配决策
 * MemberSelector 作出的成员分配决定
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDecision {

    /**
     * 计划内任务 ID
     */
    private String planTaskId;

    /**
     * 选中的 Character ID
     */
    private String selectedCharacterId;

    /**
     * 选中成员的角色名称
     */
    private String selectedMemberRole;

    /**
     * 选择原因
     */
    private String selectionReason;

    /**
     * 选择置信度
     */
    private double confidence;

    /**
     * 决策时间
     */
    private LocalDateTime decisionAt;
}

package org.dragon.workspace.service.task.arrangement.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员选择结果
 * 包含所有任务的分配决策及失败信息
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSelectionResult {

    /**
     * 分配决策列表
     */
    private List<AssignmentDecision> decisions;

    /**
     * 未分配的任务 ID 列表
     */
    private List<String> unassignedPlanTaskIds;

    /**
     * 分配失败的映射 (planTaskId -> 失败原因)
     */
    private Map<String, String> assignmentFailures;

    /**
     * 选择时间戳
     */
    private LocalDateTime selectionTimestamp;

    /**
     * 判断是否所有任务都已分配
     */
    public boolean isFullyAssigned() {
        return unassignedPlanTaskIds == null || unassignedPlanTaskIds.isEmpty();
    }
}

package org.dragon.workspace.task.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量成员选择输入
 * 用于一次性向 MemberSelector 传递多个任务和候选成员进行分配
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMemberSelectionInput {

    /**
     * Workspace ID
     */
    private String workspaceId;

    /**
     * 待分配任务计划列表
     */
    private List<PlanSummary> plans;

    /**
     * 可用成员列表
     */
    private List<MemberInfo> availableMembers;

    /**
     * 已排除的 Character ID 列表
     */
    private List<String> excludedCharacterIds;

    /**
     * 任务计划摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanSummary {
        private String planTaskId;
        private String taskName;
        private String taskDescription;
        private String expectedOutput;
        private List<String> dependencyPlanTaskIds;
    }

    /**
     * 成员信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String characterId;
        private String role;
        private String description;
    }
}
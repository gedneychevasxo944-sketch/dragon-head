package org.dragon.workspace.service.task.arrangement.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 成员选择输入
 * 用于向 MemberSelector Character 传递任务信息以选择合适的执行者
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSelectionInput {

    /**
     * 计划内任务 ID（LLM 输出的临时 ID）
     */
    private String planTaskId;

    /**
     * 子任务名称
     */
    private String taskName;

    /**
     * 子任务描述
     */
    private String taskDescription;

    /**
     * 预期输出描述
     */
    private String expectedOutput;

    /**
     * 所需能力/技能标签
     */
    private List<String> requiredCapabilities;

    /**
     * 排除的 Character ID 列表（已被其他任务占用的成员）
     */
    private List<String> excludeCharacterIds;

    /**
     * 依赖的任务名称列表
     */
    private List<String> dependencyTaskNames;

    /**
     * 父任务 ID
     */
    private String parentTaskId;
}

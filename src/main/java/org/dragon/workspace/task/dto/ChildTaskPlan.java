package org.dragon.workspace.task.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 子任务计划
 * 由 ProjectManager 分解任务后返回的子任务结构
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildTaskPlan {

    /**
     * 计划内任务 ID（LLM 输出的临时 ID，用于建立依赖关系）
     */
    private String planTaskId;

    /**
     * 子任务名称
     */
    private String name;

    /**
     * 子任务描述
     */
    private String description;

    /**
     * 分配的执行者 Character ID
     */
    private String characterId;

    /**
     * 执行者角色名称
     */
    private String characterRole;

    /**
     * 依赖的计划内任务 ID 列表（LLM 输出）
     */
    private List<String> dependencyPlanTaskIds;

    /**
     * 依赖的实际任务 ID 列表（解析后填充）
     */
    private List<String> dependencyTaskIds;

    /**
     * 是否需要用户输入
     */
    private boolean needsUserInput;

    /**
     * 预期输出描述
     */
    private String expectedOutput;
}

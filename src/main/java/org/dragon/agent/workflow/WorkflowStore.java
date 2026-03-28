package org.dragon.agent.workflow;

import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowStore 工作流存储接口
 * 负责工作流定义的持久化存储
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkflowStore extends Store {

    /**
     * 保存工作流
     *
     * @param workflow 工作流定义
     * @return 保存后的工作流
     */
    Workflow save(Workflow workflow);

    /**
     * 根据 ID 查询
     *
     * @param id 工作流 ID
     * @return Optional 工作流
     */
    Optional<Workflow> findById(String id);

    /**
     * 根据名称查询
     *
     * @param name 工作流名称
     * @return Optional 工作流
     */
    Optional<Workflow> findByName(String name);

    /**
     * 查询所有工作流
     *
     * @return 工作流列表
     */
    List<Workflow> findAll();

    /**
     * 根据 Character ID 查询关联的工作流
     *
     * @param characterId Character ID
     * @return 工作流列表
     */
    List<Workflow> findByCharacterId(String characterId);

    /**
     * 删除工作流
     *
     * @param id 工作流 ID
     * @return 是否删除成功
     */
    boolean delete(String id);

    /**
     * 获取总数
     *
     * @return 总数
     */
    int count();

    /**
     * 清除所有工作流
     */
    void clear();

    // ==================== 执行状态存储 ====================

    /**
     * 保存执行状态
     *
     * @param state 执行状态
     * @return 保存后的状态
     */
    WorkflowState saveState(WorkflowState state);

    /**
     * 根据执行 ID 查询状态
     *
     * @param executionId 执行 ID
     * @return Optional 状态
     */
    Optional<WorkflowState> findStateByExecutionId(String executionId);

    /**
     * 查询正在执行的工作流状态
     *
     * @return 状态列表
     */
    List<WorkflowState> findRunningStates();

    /**
     * 删除执行状态
     *
     * @param executionId 执行 ID
     * @return 是否删除成功
     */
    boolean deleteState(String executionId);

    /**
     * 清除所有执行状态
     */
    void clearStates();
}
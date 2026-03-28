package org.dragon.task;

import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * Task 存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface TaskStore extends Store {

    /**
     * 保存任务
     *
     * @param task 任务
     */
    void save(Task task);

    /**
     * 更新任务
     *
     * @param task 任务
     */
    void update(Task task);

    /**
     * 删除任务
     *
     * @param id 任务 ID
     */
    void delete(String id);

    /**
     * 根据 ID 查询任务
     *
     * @param id 任务 ID
     * @return 任务
     */
    Optional<Task> findById(String id);

    /**
     * 根据工作空间 ID 查询任务列表
     *
     * @param workspaceId 工作空间 ID
     * @return 任务列表
     */
    List<Task> findByWorkspaceId(String workspaceId);

    /**
     * 根据父任务 ID 查询子任务列表
     *
     * @param parentTaskId 父任务 ID
     * @return 子任务列表
     */
    List<Task> findByParentTaskId(String parentTaskId);

    /**
     * 根据状态查询任务列表
     *
     * @param status 任务状态
     * @return 任务列表
     */
    List<Task> findByStatus(TaskStatus status);

    /**
     * 根据执行者 Character ID 查询任务列表
     *
     * @param characterId Character ID
     * @return 任务列表
     */
    List<Task> findByCharacterId(String characterId);

    /**
     * 根据创建者 ID 查询任务列表
     *
     * @param creatorId 创建者 ID
     * @return 任务列表
     */
    List<Task> findByCreatorId(String creatorId);

    /**
     * 根据协作会话 ID 查询任务列表
     *
     * @param collaborationSessionId 协作会话 ID
     * @return 任务列表
     */
    List<Task> findByCollaborationSessionId(String collaborationSessionId);

    /**
     * 查询等待中的任务
     *
     * @param workspaceId 工作空间 ID
     * @return 等待中的任务列表
     */
    List<Task> findWaitingTasksByWorkspaceId(String workspaceId);

    /**
     * 检查任务是否存在
     *
     * @param id 任务 ID
     * @return 是否存在
     */
    boolean exists(String id);

    /**
     * 查询可执行的子任务（处于 PENDING 状态且依赖已满足）
     *
     * @param parentTaskId 父任务 ID
     * @return 可执行的子任务列表
     */
    List<Task> findRunnableChildTasks(String parentTaskId);

    /**
     * 查询等待特定依赖任务 ID 的任务列表
     *
     * @param dependencyTaskId 依赖的任务 ID
     * @return 等待该依赖的任务列表
     */
    List<Task> findWaitingTasksByDependencyTaskId(String dependencyTaskId);
}

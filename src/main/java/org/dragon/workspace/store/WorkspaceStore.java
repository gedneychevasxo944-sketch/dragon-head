package org.dragon.workspace.store;

import org.dragon.store.Store;
import org.dragon.workspace.Workspace;

import java.util.List;
import java.util.Optional;

/**
 * WorkspaceStore 工作空间存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface WorkspaceStore extends Store {

    /**
     * 保存工作空间
     *
     * @param workspace 工作空间
     */
    void save(Workspace workspace);

    /**
     * 更新工作空间
     *
     * @param workspace 工作空间
     */
    void update(Workspace workspace);

    /**
     * 删除工作空间
     *
     * @param id 工作空间 ID
     */
    void delete(String id);

    /**
     * 根据 ID 获取工作空间
     *
     * @param id 工作空间 ID
     * @return Optional 工作空间
     */
    Optional<Workspace> findById(String id);

    /**
     * 获取所有工作空间
     *
     * @return 工作空间列表
     */
    List<Workspace> findAll();

    /**
     * 根据状态获取工作空间列表
     *
     * @param status 工作空间状态
     * @return 工作空间列表
     */
    List<Workspace> findByStatus(Workspace.Status status);

    /**
     * 根据所有者获取工作空间列表
     *
     * @param owner 所有者 ID
     * @return 工作空间列表
     */
    List<Workspace> findByOwner(String owner);

    /**
     * 根据 ID 列表获取工作空间
     *
     * @param ids 工作空间 ID 列表
     * @return 工作空间列表
     */
    List<Workspace> findByIds(List<String> ids);

    /**
     * 根据 ID 列表和状态获取工作空间
     *
     * @param ids 工作空间 ID 列表
     * @param status 工作空间状态
     * @return 工作空间列表
     */
    List<Workspace> findByIdsAndStatus(List<String> ids, Workspace.Status status);

    /**
     * 检查工作空间是否存在
     *
     * @param id 工作空间 ID
     * @return 是否存在
     */
    boolean exists(String id);

    /**
     * 获取工作空间数量
     *
     * @return 数量
     */
    int count();
}

package org.dragon.workspace.material;

import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * MaterialStore 物料存储接口
 *
 * @author wyj
 * @version 1.0
 */
public interface MaterialStore extends Store {

    /**
     * 保存物料元数据
     *
     * @param material 物料
     */
    void save(Material material);

    /**
     * 更新物料元数据
     *
     * @param material 物料
     */
    void update(Material material);

    /**
     * 删除物料
     *
     * @param id 物料 ID
     */
    void delete(String id);

    /**
     * 根据 ID 获取物料
     *
     * @param id 物料 ID
     * @return Optional 物料
     */
    Optional<Material> findById(String id);

    /**
     * 根据工作空间 ID 获取物料列表
     *
     * @param workspaceId 工作空间 ID
     * @return 物料列表
     */
    List<Material> findByWorkspaceId(String workspaceId);

    /**
     * 根据名称获取物料列表
     *
     * @param workspaceId 工作空间 ID
     * @param name 物料名称
     * @return 物料列表
     */
    List<Material> findByName(String workspaceId, String name);

    /**
     * 检查物料是否存在
     *
     * @param id 物料 ID
     * @return 是否存在
     */
    boolean exists(String id);
}

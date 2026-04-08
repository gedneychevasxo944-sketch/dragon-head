package org.dragon.workspace.member;

import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * TeamPosition 存储接口
 *
 * @author qieqie
 * @version 1.0
 */
public interface TeamPositionStore extends Store {

    /**
     * 保存岗位
     */
    void save(TeamPosition position);

    /**
     * 更新岗位
     */
    void update(TeamPosition position);

    /**
     * 删除岗位
     */
    void delete(String id);

    /**
     * 根据 ID 查询
     */
    Optional<TeamPosition> findById(String id);

    /**
     * 根据 workspaceId 查询所有岗位
     */
    List<TeamPosition> findByWorkspaceId(String workspaceId);

    /**
     * 根据 workspaceId 和 roleName 查询
     */
    Optional<TeamPosition> findByWorkspaceIdAndRoleName(String workspaceId, String roleName);
}

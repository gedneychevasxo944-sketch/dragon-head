package org.dragon.memv2.core;

import java.util.List;

/**
 * 记忆索引服务接口
 * 负责管理记忆索引的创建、查询和更新
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryIndexService {
    /**
     * 重建指定角色的记忆索引
     *
     * @param characterId 角色ID
     */
    void rebuildCharacterIndex(String characterId);

    /**
     * 重建指定工作空间的记忆索引
     *
     * @param workspaceId 工作空间ID
     */
    void rebuildWorkspaceIndex(String workspaceId);

    /**
     * 查询角色记忆索引
     *
     * @param characterId 角色ID
     * @return 记忆索引条目列表
     */
    List<MemoryIndexItem> queryCharacterIndex(String characterId);

    /**
     * 查询工作空间记忆索引
     *
     * @param workspaceId 工作空间ID
     * @return 记忆索引条目列表
     */
    List<MemoryIndexItem> queryWorkspaceIndex(String workspaceId);

    /**
     * 按关键词搜索角色记忆索引
     *
     * @param characterId 角色ID
     * @param keyword 搜索关键词
     * @return 匹配的记忆索引条目列表
     */
    List<MemoryIndexItem> searchCharacterIndex(String characterId, String keyword);

    /**
     * 按关键词搜索工作空间记忆索引
     *
     * @param workspaceId 工作空间ID
     * @param keyword 搜索关键词
     * @return 匹配的记忆索引条目列表
     */
    List<MemoryIndexItem> searchWorkspaceIndex(String workspaceId, String keyword);
}

package org.dragon.config.store;

import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 配置存储抽象接口
 *
 * <p>扁平化存储结构：
 * <ul>
 *   <li>id = {scopeBits}:{workspaceId}:{characterId}:{toolId}:{skillId}:{configKey}</li>
 *   <li>通过 scopeBits 位掩码标识激活的层级</li>
 * </ul>
 */
public interface ConfigStore extends Store {

    /**
     * 保存配置（使用扁平化结构）
     *
     * @param configKey 配置键
     * @param value 配置值
     * @param scopeBits 层级位掩码
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     * @param toolId Tool ID
     * @param skillId Skill ID
     */
    void set(String configKey, Object value, int scopeBits,
             String workspaceId, String characterId, String toolId, String skillId);

    /**
     * 查询配置（使用扁平化结构）
     *
     * @param configKey 配置键
     * @param scopeBits 层级位掩码
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     * @param toolId Tool ID
     * @param skillId Skill ID
     * @return 配置值
     */
    Optional<Object> get(String configKey, int scopeBits,
                         String workspaceId, String characterId, String toolId, String skillId);

    /**
     * 清空所有配置
     */
    void clear();

    /**
     * 获取所有配置项（用于列表展示）
     *
     * @return 配置项列表
     */
    List<ConfigStoreItem> listAll();

    /**
     * 配置存储项（用于列表查询）
     */
    record ConfigStoreItem(
            String configKey,
            int scopeBits,
            String workspaceId,
            String characterId,
            String toolId,
            String skillId,
            Object value
    ) {}
}
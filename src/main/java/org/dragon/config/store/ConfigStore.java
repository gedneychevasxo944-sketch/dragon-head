package org.dragon.config.store;

import org.dragon.config.enums.ConfigLevel;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 配置存储抽象接口
 *
 * <p>扁平化存储结构：
 * <ul>
 *   <li>id = {scopeBit}:{workspaceId}:{characterId}:{toolId}:{skillId}:{memoryId}:{configKey}</li>
 *   <li>通过 ConfigLevel 标识粒度</li>
 * </ul>
 */
public interface ConfigStore extends Store {

    /**
     * 保存配置
     *
     * @param level 粒度
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     * @param toolId Tool ID
     * @param skillId Skill ID
     * @param memoryId Memory ID
     * @param configKey 配置键
     * @param value 配置值
     */
    void set(ConfigLevel level, String workspaceId, String characterId,
             String toolId, String skillId, String memoryId,
             String configKey, Object value);

    /**
     * 查询配置
     *
     * @param level 粒度
     * @param workspaceId Workspace ID
     * @param characterId Character ID
     * @param toolId Tool ID
     * @param skillId Skill ID
     * @param memoryId Memory ID
     * @param configKey 配置键
     * @return 配置值
     */
    Optional<Object> get(ConfigLevel level, String workspaceId, String characterId,
                         String toolId, String skillId, String memoryId,
                         String configKey);

    /**
     * 查询配置（简化版，只有 workspaceId）
     */
    Optional<Object> get(ConfigLevel level, String workspaceId, String configKey);

    /**
     * 查询配置（只有 workspaceId 和 characterId）
     */
    Optional<Object> get(ConfigLevel level, String workspaceId, String characterId, String configKey);

    /**
     * 删除配置
     */
    void delete(ConfigLevel level, String workspaceId, String characterId,
                String toolId, String skillId, String memoryId, String configKey);

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
     * 获取指定粒度的所有配置项
     *
     * @param level 粒度
     * @return 配置项列表
     */
    List<ConfigStoreItem> listByLevel(ConfigLevel level);

    /**
     * 获取指定配置键在 GLOBAL 级别的元数据
     *
     * @param configKey 配置键
     * @return 元数据（name, description, validationRules, options）
     */
    ConfigMetadata getMetadata(String configKey);

    /**
     * 获取所有配置项的元数据列表（从 GLOBAL 级别）
     *
     * @return 所有配置键的元数据列表
     */
    List<ConfigMetadata> listMetadata();

    /**
     * 配置存储项（用于列表查询）
     */
    record ConfigStoreItem(
            ConfigLevel level,
            String workspaceId,
            String characterId,
            String toolId,
            String skillId,
            String memoryId,
            String configKey,
            Object value
    ) {}

    /**
     * 配置元数据（从 GLOBAL 级别行获取）
     */
    record ConfigMetadata(
            String configKey,
            String name,
            String description,
            String validationRules,
            String options,
            String valueType,
            Object defaultValue
    ) {}
}
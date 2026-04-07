package org.dragon.config.store;

import org.dragon.config.enums.ConfigLevel;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * OBSERVER 配置存储抽象接口
 *
 * <p>OBSERVER 体系的配置存储，使用 config_store_observer 表。
 */
public interface ObserverConfigStore extends Store {

    /**
     * 保存 OBSERVER 配置
     */
    void set(String observerId, ConfigLevel level, String workspaceId, String characterId,
             String toolId, String skillId, String memoryId,
             String configKey, Object value);

    /**
     * 查询 OBSERVER 配置
     */
    Optional<Object> get(String observerId, ConfigLevel level, String workspaceId, String characterId,
                         String toolId, String skillId, String memoryId,
                         String configKey);

    /**
     * 查询 OBSERVER 配置（简化版，只有 workspaceId）
     */
    Optional<Object> get(String observerId, ConfigLevel level, String workspaceId, String configKey);

    /**
     * 查询 OBSERVER 配置（只有 workspaceId 和 characterId）
     */
    Optional<Object> get(String observerId, ConfigLevel level, String workspaceId, String characterId, String configKey);

    /**
     * 删除 OBSERVER 配置
     */
    void delete(String observerId, ConfigLevel level, String workspaceId, String characterId,
                String toolId, String skillId, String memoryId, String configKey);

    /**
     * 清空所有 OBSERVER 配置
     */
    void clear();

    /**
     * 清空指定 OBSERVER 的所有配置
     */
    void clearByObserver(String observerId);

    /**
     * 获取所有 OBSERVER 配置项
     */
    List<ObserverConfigStoreItem> listAll();

    /**
     * 获取指定 OBSERVER 的所有配置项
     */
    List<ObserverConfigStoreItem> listByObserver(String observerId);

    /**
     * 获取指定粒度的所有 OBSERVER 配置项
     */
    List<ObserverConfigStoreItem> listByLevel(ConfigLevel level);

    /**
     * OBSERVER 配置存储项
     */
    record ObserverConfigStoreItem(
            String observerId,
            ConfigLevel level,
            String workspaceId,
            String characterId,
            String toolId,
            String skillId,
            String memoryId,
            String configKey,
            Object value
    ) {}
}
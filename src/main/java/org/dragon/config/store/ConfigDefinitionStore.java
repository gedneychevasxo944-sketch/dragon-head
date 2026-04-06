package org.dragon.config.store;

import org.dragon.datasource.entity.ConfigDefinition;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * 配置项定义存储接口
 *
 * <p>管理 ConfigDefinition 实体，存储配置项的元数据：
 * <ul>
 *   <li>配置项所属的作用域</li>
 *   <li>值类型</li>
 *   <li>描述信息</li>
 *   <li>默认值（对应 hardcoded 默认值）</li>
 * </ul>
 */
public interface ConfigDefinitionStore extends Store {

    /**
     * 保存配置项定义
     *
     * @param definition 配置项定义
     */
    void save(ConfigDefinition definition);

    /**
     * 更新配置项定义
     *
     * @param definition 配置项定义
     */
    void update(ConfigDefinition definition);

    /**
     * 根据 ID 查询
     *
     * @param id 配置项定义 ID
     * @return Optional 配置项定义
     */
    Optional<ConfigDefinition> findById(String id);

    /**
     * 根据作用域类型查询所有定义
     *
     * @param scopeType 作用域类型
     * @return 配置项定义列表
     */
    List<ConfigDefinition> findByScopeType(String scopeType);

    /**
     * 根据作用域类型和配置键查询
     *
     * @param scopeType 作用域类型
     * @param configKey 配置键
     * @return Optional 配置项定义
     */
    Optional<ConfigDefinition> findByScopeTypeAndKey(String scopeType, String configKey);

    /**
     * 查询所有配置项定义
     *
     * @return 所有配置项定义
     */
    List<ConfigDefinition> findAll();

    /**
     * 删除配置项定义
     *
     * @param id 配置项定义 ID
     */
    void delete(String id);

    /**
     * 检查是否存在
     *
     * @param scopeType 作用域类型
     * @param configKey 配置键
     * @return 是否存在
     */
    boolean exists(String scopeType, String configKey);
}
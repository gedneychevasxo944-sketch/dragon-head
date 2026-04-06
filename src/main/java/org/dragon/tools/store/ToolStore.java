package org.dragon.tools.store;

import org.dragon.store.Store;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ToolStore 工具存储接口
 * 注意：AgentTool包含执行逻辑无法持久化，此Store仅存储工具元数据
 */
public interface ToolStore extends Store {

    /**
     * 保存工具元数据
     */
    void save(Map<String, Object> toolMetadata);

    /**
     * 更新工具元数据
     */
    void update(Map<String, Object> toolMetadata);

    /**
     * 删除工具
     */
    void delete(String name);

    /**
     * 根据名称获取工具元数据
     */
    Optional<Map<String, Object>> findByName(String name);

    /**
     * 获取所有工具元数据
     */
    List<Map<String, Object>> findAll();

    /**
     * 获取所有启用的工具元数据
     */
    List<Map<String, Object>> findEnabled();

    /**
     * 检查工具是否存在
     */
    boolean exists(String name);

    /**
     * 获取工具数量
     */
    int count();
}
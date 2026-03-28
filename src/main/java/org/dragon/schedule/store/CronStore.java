package org.dragon.schedule.store;

import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * Cron 存储抽象接口
 * 对应架构文档中的 CronStore
 */
public interface CronStore extends Store {

    /**
     * 保存 Cron 定义
     *
     * @param definition Cron 定义
     */
    void save(CronDefinition definition);

    /**
     * 更新 Cron 定义
     *
     * @param definition Cron 定义
     */
    void update(CronDefinition definition);

    /**
     * 删除 Cron 定义
     *
     * @param id Cron ID
     */
    void delete(String id);

    /**
     * 根据 ID 查找
     *
     * @param id Cron ID
     * @return Optional<CronDefinition>
     */
    Optional<CronDefinition> findById(String id);

    /**
     * 查找所有
     *
     * @return List<CronDefinition>
     */
    List<CronDefinition> findAll();

    /**
     * 根据状态查找
     *
     * @param status Cron 状态
     * @return List<CronDefinition>
     */
    List<CronDefinition> findByStatus(CronStatus status);

    /**
     * 批量保存
     *
     * @param definitions Cron 定义列表
     */
    void batchSave(List<CronDefinition> definitions);

    /**
     * 检查是否存在
     *
     * @param id Cron ID
     * @return boolean
     */
    boolean exists(String id);

    /**
     * 计数
     *
     * @return long
     */
    long count();

    /**
     * 根据状态计数
     *
     * @param status Cron 状态
     * @return long
     */
    long countByStatus(CronStatus status);
}

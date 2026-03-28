package org.dragon.schedule.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;
import org.dragon.schedule.entity.ValidationResult;
import org.dragon.schedule.parser.CronExpression;
import org.dragon.schedule.store.CronStore;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cron 服务实现
 * 提供 Cron 任务的业务逻辑层服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CronService {

    private final CronScheduler scheduler;
    private final StoreFactory storeFactory;

    private CronStore getCronStore() {
        return storeFactory.get(CronStore.class);
    }

    /**
     * 创建 Cron 任务
     *
     * @param definition Cron 定义
     * @return Cron ID
     */
    public String createCron(CronDefinition definition) {
        // 验证
        ValidationResult validation = definition.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid cron definition: " + validation.getErrorMessage());
        }

        // 验证 Cron 表达式
        validateCronExpression(definition.getCronExpression());

        // 生成 ID
        if (definition.getId() == null) {
            definition.setId(UUID.randomUUID().toString().replace("-", ""));
        }

        // 设置时间戳
        long now = System.currentTimeMillis();
        if (definition.getCreatedAt() == null) {
            definition.setCreatedAt(now);
        }
        definition.setUpdatedAt(now);

        // 设置默认状态
        if (definition.getStatus() == null) {
            definition.setStatus(CronStatus.ENABLED);
        }

        // 保存到存储
        getCronStore().save(definition);

        // 如果状态为启用，注册到调度器
        if (definition.getStatus() == CronStatus.ENABLED && scheduler.isRunning()) {
            scheduler.registerCron(definition);
        }

        log.info("Created cron job: id={}, name={}, expression={}",
                definition.getId(), definition.getName(), definition.getCronExpression());

        return definition.getId();
    }

    /**
     * 更新 Cron 任务
     *
     * @param definition Cron 定义
     */
    public void updateCron(CronDefinition definition) {
        if (definition.getId() == null) {
            throw new IllegalArgumentException("Cron ID cannot be null");
        }

        // 验证
        ValidationResult validation = definition.validate();
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid cron definition: " + validation.getErrorMessage());
        }

        // 验证 Cron 表达式
        validateCronExpression(definition.getCronExpression());

        // 检查是否存在
        Optional<CronDefinition> existing = getCronStore().findById(definition.getId());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Cron not found: " + definition.getId());
        }

        // 更新时间戳
        definition.setUpdatedAt(System.currentTimeMillis());
        definition.setCreatedAt(existing.get().getCreatedAt()); // 保持创建时间不变

        // 保存到存储
        getCronStore().update(definition);

        // 更新调度器
        scheduler.updateCron(definition);

        log.info("Updated cron job: id={}, name={}, expression={}",
                definition.getId(), definition.getName(), definition.getCronExpression());
    }

    /**
     * 删除 Cron 任务
     *
     * @param cronId Cron ID
     */
    public void deleteCron(String cronId) {
        if (cronId == null) {
            throw new IllegalArgumentException("Cron ID cannot be null");
        }

        // 检查是否存在
        Optional<CronDefinition> existing = getCronStore().findById(cronId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Cron not found: " + cronId);
        }

        // 从调度器取消注册
        scheduler.unregisterCron(cronId);

        // 从存储删除
        getCronStore().delete(cronId);

        log.info("Deleted cron job: id={}", cronId);
    }

    /**
     * 暂停 Cron 任务
     *
     * @param cronId Cron ID
     */
    public void pauseCron(String cronId) {
        if (cronId == null) {
            throw new IllegalArgumentException("Cron ID cannot be null");
        }

        // 检查是否存在
        Optional<CronDefinition> existing = getCronStore().findById(cronId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Cron not found: " + cronId);
        }

        CronDefinition definition = existing.get();

        // 只有启用状态才能暂停
        if (definition.getStatus() != CronStatus.ENABLED) {
            throw new IllegalStateException("Cannot pause cron with status: " + definition.getStatus());
        }

        // 更新状态
        definition.updateStatus(CronStatus.PAUSED);
        getCronStore().update(definition);

        // 暂停调度器
        scheduler.pauseCron(cronId);

        log.info("Paused cron job: id={}", cronId);
    }

    /**
     * 恢复 Cron 任务
     *
     * @param cronId Cron ID
     */
    public void resumeCron(String cronId) {
        if (cronId == null) {
            throw new IllegalArgumentException("Cron ID cannot be null");
        }

        // 检查是否存在
        Optional<CronDefinition> existing = getCronStore().findById(cronId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Cron not found: " + cronId);
        }

        CronDefinition definition = existing.get();

        // 只有暂停状态才能恢复
        if (definition.getStatus() != CronStatus.PAUSED) {
            throw new IllegalStateException("Cannot resume cron with status: " + definition.getStatus());
        }

        // 更新状态
        definition.updateStatus(CronStatus.ENABLED);
        getCronStore().update(definition);

        // 恢复调度器
        scheduler.resumeCron(cronId);

        log.info("Resumed cron job: id={}", cronId);
    }

    /**
     * 立即触发执行
     *
     * @param cronId Cron ID
     */
    public void triggerNow(String cronId) {
        if (cronId == null) {
            throw new IllegalArgumentException("Cron ID cannot be null");
        }

        // 检查是否存在
        Optional<CronDefinition> existing = getCronStore().findById(cronId);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Cron not found: " + cronId);
        }

        scheduler.triggerNow(cronId);

        log.info("Triggered cron job manually: id={}", cronId);
    }

    /**
     * 获取 Cron 定义
     *
     * @param cronId Cron ID
     * @return Optional<CronDefinition>
     */
    public Optional<CronDefinition> getCron(String cronId) {
        return getCronStore().findById(cronId);
    }

    /**
     * 列出所有 Cron 任务
     *
     * @return List<CronDefinition>
     */
    public List<CronDefinition> listCrons() {
        return getCronStore().findAll();
    }

    /**
     * 按状态列出 Cron 任务
     *
     * @param status Cron 状态
     * @return List<CronDefinition>
     */
    public List<CronDefinition> listCronsByStatus(CronStatus status) {
        return getCronStore().findByStatus(status);
    }

    /**
     * 验证 Cron 表达式
     */
    private void validateCronExpression(String expression) {
        try {
            CronExpression.parse(expression);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + expression + ", error: " + e.getMessage());
        }
    }
}

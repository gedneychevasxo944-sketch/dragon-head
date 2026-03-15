package org.dragon.schedule.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.schedule.entity.CronDefinition;
import org.dragon.schedule.entity.CronStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 内存 Cron 存储实现
 */
@Slf4j
public class MemoryCronStore implements CronStore {

    private final Map<String, CronDefinition> store = new ConcurrentHashMap<>();

    @Override
    public void save(CronDefinition definition) {
        if (definition == null || definition.getId() == null) {
            throw new IllegalArgumentException("Cron definition or ID cannot be null");
        }
        store.put(definition.getId(), copy(definition));
        log.debug("Saved cron definition: id={}", definition.getId());
    }

    @Override
    public void update(CronDefinition definition) {
        if (definition == null || definition.getId() == null) {
            throw new IllegalArgumentException("Cron definition or ID cannot be null");
        }
        if (!store.containsKey(definition.getId())) {
            throw new IllegalArgumentException("Cron definition not found: " + definition.getId());
        }
        store.put(definition.getId(), copy(definition));
        log.debug("Updated cron definition: id={}", definition.getId());
    }

    @Override
    public void delete(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cron ID cannot be null");
        }
        store.remove(id);
        log.debug("Deleted cron definition: id={}", id);
    }

    @Override
    public Optional<CronDefinition> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        CronDefinition definition = store.get(id);
        return definition != null ? Optional.of(copy(definition)) : Optional.empty();
    }

    @Override
    public List<CronDefinition> findAll() {
        return store.values().stream()
                .map(this::copy)
                .collect(Collectors.toList());
    }

    @Override
    public List<CronDefinition> findByStatus(CronStatus status) {
        if (status == null) {
            return Collections.emptyList();
        }
        return store.values().stream()
                .filter(cron -> status.equals(cron.getStatus()))
                .map(this::copy)
                .collect(Collectors.toList());
    }

    @Override
    public void batchSave(List<CronDefinition> definitions) {
        if (definitions == null || definitions.isEmpty()) {
            return;
        }
        for (CronDefinition definition : definitions) {
            save(definition);
        }
    }

    @Override
    public boolean exists(String id) {
        if (id == null) {
            return false;
        }
        return store.containsKey(id);
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public long countByStatus(CronStatus status) {
        if (status == null) {
            return 0;
        }
        return store.values().stream()
                .filter(cron -> status.equals(cron.getStatus()))
                .count();
    }

    /**
     * 深拷贝
     */
    private CronDefinition copy(CronDefinition source) {
        if (source == null) {
            return null;
        }
        CronDefinition copy = new CronDefinition();
        copy.setId(source.getId());
        copy.setName(source.getName());
        copy.setDescription(source.getDescription());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setCronExpression(source.getCronExpression());
        copy.setTimezone(source.getTimezone());
        copy.setStartTime(source.getStartTime());
        copy.setEndTime(source.getEndTime());
        copy.setJobType(source.getJobType());
        copy.setJobHandler(source.getJobHandler());
        copy.setJobData(source.getJobData() != null ? new java.util.HashMap<>(source.getJobData()) : null);
        copy.setMisfirePolicy(source.getMisfirePolicy());
        copy.setMaxConcurrent(source.getMaxConcurrent());
        copy.setTimeoutMs(source.getTimeoutMs());
        copy.setRetryCount(source.getRetryCount());
        copy.setRetryIntervalMs(source.getRetryIntervalMs());
        copy.setStatus(source.getStatus());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setVersion(source.getVersion());
        return copy;
    }
}

package org.dragon.skill.store;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.entity.SkillEntity;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillLifecycleState;
import org.dragon.skill.model.SkillSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存实现的 Skill 存储。
 *
 * @since 1.0
 */
@Slf4j
@Component
public class MemorySkillStore implements SkillStore {

    /** 主索引：id -> SkillEntity */
    private final Map<Long, SkillEntity> idIndex = new ConcurrentHashMap<>();

    /** 名称索引：name -> id */
    private final Map<String, Long> nameIndex = new ConcurrentHashMap<>();

    /** ID 生成器 */
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public SkillEntity save(SkillEntity entity) {
        Long id = entity.getId();
        if (id == null) {
            id = idGenerator.getAndIncrement();
            entity.setId(id);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        idIndex.put(id, entity);
        nameIndex.put(entity.getName(), id);
        log.info("Skill 已保存: id={}, name={}", id, entity.getName());
        return entity;
    }

    @Override
    public SkillEntity update(SkillEntity entity) {
        Long id = entity.getId();
        if (!idIndex.containsKey(id)) {
            throw new IllegalArgumentException("Skill 不存在: id=" + id);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        idIndex.put(id, entity);
        // 名称可能已更新，需要更新索引
        nameIndex.put(entity.getName(), id);
        log.info("Skill 已更新: id={}, name={}", id, entity.getName());
        return entity;
    }

    @Override
    public void delete(Long id) {
        SkillEntity entity = idIndex.remove(id);
        if (entity != null) {
            nameIndex.remove(entity.getName());
            log.info("Skill 已删除: id={}, name={}", id, entity.getName());
        }
    }

    @Override
    public Optional<SkillEntity> findById(Long id) {
        SkillEntity entity = idIndex.get(id);
        if (entity != null && entity.getDeletedAt() == null) {
            return Optional.of(entity);
        }
        return Optional.empty();
    }

    @Override
    public Optional<SkillEntity> findByName(String name) {
        Long id = nameIndex.get(name);
        if (id == null) {
            return Optional.empty();
        }
        return findById(id);
    }

    @Override
    public List<SkillEntity> findAll() {
        return idIndex.values().stream()
                .filter(e -> e.getDeletedAt() == null)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillEntity> findBySource(SkillSource source) {
        return idIndex.values().stream()
                .filter(e -> e.getDeletedAt() == null && e.getSource() == source)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillEntity> findByCategory(SkillCategory category) {
        return idIndex.values().stream()
                .filter(e -> e.getDeletedAt() == null && e.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillEntity> findByLifecycleState(SkillLifecycleState state) {
        return idIndex.values().stream()
                .filter(e -> e.getDeletedAt() == null && e.getLifecycleState() == state)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public boolean existsByNameExcludeId(String name, Long excludeId) {
        Optional<SkillEntity> existing = findByName(name);
        return existing.isPresent() && !existing.get().getId().equals(excludeId);
    }

    @Override
    public void updateLifecycleState(Long id, SkillLifecycleState state, String error) {
        findById(id).ifPresent(entity -> {
            entity.setLifecycleState(state);
            entity.setLoadError(error);
            entity.setUpdatedAt(LocalDateTime.now());
            idIndex.put(id, entity);
            log.info("Skill 状态已更新: id={}, name={}, state={}", id, entity.getName(), state);
        });
    }

    @Override
    public void softDelete(Long id) {
        findById(id).ifPresent(entity -> {
            entity.setDeletedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            idIndex.put(id, entity);
            nameIndex.remove(entity.getName());
            log.info("Skill 已软删除: id={}, name={}", id, entity.getName());
        });
    }
}

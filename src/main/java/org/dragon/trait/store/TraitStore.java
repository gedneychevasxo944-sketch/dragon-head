package org.dragon.trait.store;

import org.dragon.datasource.entity.TraitEntity;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * TraitStore 特征片段存储接口
 */
public interface TraitStore extends Store {

    /**
     * 保存 Trait
     */
    void save(TraitEntity trait);

    /**
     * 更新 Trait
     */
    void update(TraitEntity trait);

    /**
     * 根据 ID 查找
     */
    Optional<TraitEntity> findById(Long id);

    /**
     * 批量查找
     */
    List<TraitEntity> findByIds(List<Long> ids);

    /**
     * 查询所有 Trait
     */
    List<TraitEntity> findAll();

    /**
     * 按类型查询
     */
    List<TraitEntity> findByType(String type);

    /**
     * 按分类查询
     */
    List<TraitEntity> findByCategory(String category);

    /**
     * 搜索 Trait（按名称或描述）
     */
    List<TraitEntity> search(String keyword);

    /**
     * 统计总数
     */
    long count();

    /**
     * 删除 Trait
     */
    void delete(Long id);

    /**
     * 增加引用计数
     */
    void incrementUsedByCount(Long id);

    /**
     * 减少引用计数
     */
    void decrementUsedByCount(Long id);
}

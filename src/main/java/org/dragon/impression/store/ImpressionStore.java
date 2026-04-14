package org.dragon.impression.store;

import org.dragon.impression.entity.ImpressionEntity;
import org.dragon.impression.enums.ImpressionType;
import org.dragon.store.Store;

import java.util.List;
import java.util.Optional;

/**
 * ImpressionStore 印象存储接口
 */
public interface ImpressionStore extends Store {

    /**
     * 保存印象
     */
    void save(ImpressionEntity impression);

    /**
     * 更新印象
     */
    void update(ImpressionEntity impression);

    /**
     * 删除印象
     */
    void delete(String id);

    /**
     * 根据ID查询
     */
    Optional<ImpressionEntity> findById(String id);

    /**
     * 根据源实体查询所有印象
     */
    List<ImpressionEntity> findBySource(ImpressionType sourceType, String sourceId);

    /**
     * 根据目标实体查询所有印象
     */
    List<ImpressionEntity> findByTarget(ImpressionType targetType, String targetId);

    /**
     * 根据源和目标查询印象
     */
    Optional<ImpressionEntity> findBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                                      ImpressionType targetType, String targetId);

    /**
     * 根据源和目标删除印象
     */
    void deleteBySourceAndTarget(ImpressionType sourceType, String sourceId,
                                  ImpressionType targetType, String targetId);
}

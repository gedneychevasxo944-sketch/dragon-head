package org.dragon.template.store;

import org.dragon.permission.enums.ResourceType;
import org.dragon.store.Store;
import org.dragon.datasource.entity.TemplateMarkEntity;

import java.util.List;
import java.util.Optional;

/**
 * TemplateMarkStore 模板标记存储接口
 *
 * @author yijunw
 */
public interface TemplateMarkStore extends Store {

    /**
     * 保存模板标记
     *
     * @param templateMark 模板标记实体
     */
    void save(TemplateMarkEntity templateMark);

    /**
     * 更新模板标记
     *
     * @param templateMark 模板标记实体
     */
    void update(TemplateMarkEntity templateMark);

    /**
     * 根据 ID 查询
     *
     * @param id 主键 ID
     * @return 模板标记实体
     */
    Optional<TemplateMarkEntity> findById(String id);

    /**
     * 根据资源类型和资源 ID 查询
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 模板标记实体
     */
    Optional<TemplateMarkEntity> findByResource(ResourceType resourceType, String resourceId);

    /**
     * 查询全部模板标记
     *
     * @return 模板标记列表
     */
    List<TemplateMarkEntity> findAll();

    /**
     * 根据资源类型查询
     *
     * @param resourceType 资源类型
     * @return 模板标记列表
     */
    List<TemplateMarkEntity> findByResourceType(ResourceType resourceType);

    /**
     * 根据分类查询
     *
     * @param category 分类
     * @return 模板标记列表
     */
    List<TemplateMarkEntity> findByCategory(String category);

    /**
     * 增加派生次数
     *
     * @param id 主键 ID
     */
    void incrementUsageCount(String id);

    /**
     * 删除模板标记
     *
     * @param id 主键 ID
     */
    void delete(String id);

    /**
     * 根据资源类型和资源 ID 删除
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     */
    void deleteByResource(ResourceType resourceType, String resourceId);
}

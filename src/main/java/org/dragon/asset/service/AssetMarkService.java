package org.dragon.asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.ExpertEntity;
import org.dragon.expert.store.ExpertStore;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AssetMarkService 资产标记服务
 *
 * <p>统一管理资产的 Expert 和 Builtin 标记。
 * Expert 标记用于用户创建的模板资产，Builtin 标记用于系统预置资产。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetMarkService {

    private final StoreFactory storeFactory;

    private ExpertStore getExpertStore() {
        return storeFactory.get(ExpertStore.class);
    }

    // ==================== 标记操作 ====================

    /**
     * 标记为 Expert
     *
     * @param resourceType    资源类型
     * @param resourceId      资源 ID
     * @param category        分类
     * @param preview         预览文本
     * @param targetAudience  目标用户群体
     * @return Expert 标记实体
     */
    public ExpertEntity markAsExpert(ResourceType resourceType, String resourceId,
                                     String category, String preview, String targetAudience) {
        // 如果已经标记过，直接返回
        Optional<ExpertEntity> existing = getExpertStore().findByResource(resourceType, resourceId);
        if (existing.isPresent()) {
            ExpertEntity mark = existing.get();
            if (mark.getMarkType() == ExpertEntity.MarkType.EXPERT) {
                log.info("[AssetMarkService] Asset already marked as expert: {} {}", resourceType, resourceId);
                return mark;
            }
            // 如果是 BUILTIN 转成 EXPERT，先删除再创建
            getExpertStore().deleteByResource(resourceType, resourceId);
        }

        ExpertEntity mark = ExpertEntity.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .markType(ExpertEntity.MarkType.EXPERT)
                .category(category)
                .preview(preview)
                .targetAudience(targetAudience)
                .usageCount(0)
                .build();
        getExpertStore().save(mark);

        log.info("[AssetMarkService] Marked as expert: {} {}", resourceType, resourceId);
        return mark;
    }

    /**
     * 标记为 Builtin
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return Expert 标记实体
     */
    public ExpertEntity markAsBuiltin(ResourceType resourceType, String resourceId) {
        // 如果已经标记过，直接返回
        Optional<ExpertEntity> existing = getExpertStore().findByResource(resourceType, resourceId);
        if (existing.isPresent()) {
            ExpertEntity mark = existing.get();
            if (mark.getMarkType() == ExpertEntity.MarkType.BUILTIN) {
                log.info("[AssetMarkService] Asset already marked as builtin: {} {}", resourceType, resourceId);
                return mark;
            }
            // 如果是 EXPERT 转成 BUILTIN，先删除再创建
            getExpertStore().deleteByResource(resourceType, resourceId);
        }

        ExpertEntity mark = ExpertEntity.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .markType(ExpertEntity.MarkType.BUILTIN)
                .category(null)
                .preview(null)
                .targetAudience(null)
                .usageCount(0)
                .build();
        getExpertStore().save(mark);

        log.info("[AssetMarkService] Marked as builtin: {} {}", resourceType, resourceId);
        return mark;
    }

    /**
     * 取消标记（同时取消 EXPERT 和 BUILTIN）
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     */
    public void unmark(ResourceType resourceType, String resourceId) {
        getExpertStore().deleteByResource(resourceType, resourceId);
        log.info("[AssetMarkService] Unmarked: {} {}", resourceType, resourceId);
    }

    // ==================== 查询操作 ====================

    /**
     * 获取资产标记信息
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 标记信息（不存在返回空）
     */
    public Optional<ExpertEntity> getMark(ResourceType resourceType, String resourceId) {
        return getExpertStore().findByResource(resourceType, resourceId);
    }

    /**
     * 检查资产是否有指定标记类型
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @param markType     标记类型
     * @return 是否有该标记
     */
    public boolean hasMarkType(ResourceType resourceType, String resourceId, ExpertEntity.MarkType markType) {
        return getExpertStore().findByResource(resourceType, resourceId)
                .map(mark -> mark.getMarkType() == markType)
                .orElse(false);
    }

    /**
     * 检查资产是否是 Expert
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 是否是 Expert
     */
    public boolean isExpert(ResourceType resourceType, String resourceId) {
        return hasMarkType(resourceType, resourceId, ExpertEntity.MarkType.EXPERT);
    }

    /**
     * 检查资产是否是 Builtin
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 是否是 Builtin
     */
    public boolean isBuiltin(ResourceType resourceType, String resourceId) {
        return hasMarkType(resourceType, resourceId, ExpertEntity.MarkType.BUILTIN);
    }

    /**
     * 列出指定标记类型的所有资产
     *
     * @param markType     标记类型
     * @param resourceType 资源类型（null 表示全部）
     * @param category     分类（仅 EXPERT 有效，null 表示全部）
     * @return 标记列表
     */
    public List<ExpertEntity> listByMarkType(ExpertEntity.MarkType markType, ResourceType resourceType, String category) {
        List<ExpertEntity> marks;
        if (resourceType != null) {
            marks = getExpertStore().findByResourceType(resourceType);
        } else {
            marks = getExpertStore().findAll();
        }

        // 按 markType 过滤
        marks = marks.stream()
                .filter(m -> m.getMarkType() == markType)
                .toList();

        // 按分类过滤（仅 EXPERT 有意义）
        if (category != null && !category.isBlank() && markType == ExpertEntity.MarkType.EXPERT) {
            marks = marks.stream()
                    .filter(m -> category.equals(m.getCategory()))
                    .toList();
        }

        return marks;
    }

    /**
     * 列出所有 Expert 资产
     *
     * @param resourceType 资源类型（null 表示全部）
     * @param category     分类（null 表示全部）
     * @return Expert 标记列表
     */
    public List<ExpertEntity> listExperts(ResourceType resourceType, String category) {
        return listByMarkType(ExpertEntity.MarkType.EXPERT, resourceType, category);
    }

    /**
     * 列出所有 Builtin 资产
     *
     * @param resourceType 资源类型（null 表示全部）
     * @return Builtin 标记列表
     */
    public List<ExpertEntity> listBuiltins(ResourceType resourceType) {
        return listByMarkType(ExpertEntity.MarkType.BUILTIN, resourceType, null);
    }

    // ==================== 批量过滤操作 ====================

    /**
     * 从资产 ID 列表中过滤掉有指定标记的资产
     *
     * @param resourceType 资源类型
     * @param assetIds     资产 ID 列表
     * @param markType     标记类型
     * @return 排除有标记资产后的 ID 集合
     */
    public Set<String> filterByMarkType(ResourceType resourceType, Collection<String> assetIds,
                                        ExpertEntity.MarkType markType) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<ExpertEntity> marks = getExpertStore().findByResourceType(resourceType);
        Set<String> markedIds = marks.stream()
                .filter(m -> m.getMarkType() == markType)
                .map(ExpertEntity::getResourceId)
                .collect(Collectors.toSet());

        return assetIds.stream()
                .filter(id -> !markedIds.contains(id))
                .collect(Collectors.toSet());
    }

    /**
     * 从资产 ID 列表中过滤掉 Expert 资产
     *
     * @param resourceType 资源类型
     * @param assetIds     资产 ID 列表
     * @return 排除 Expert 后的 ID 集合
     */
    public Set<String> filterOutExperts(ResourceType resourceType, Collection<String> assetIds) {
        return filterByMarkType(resourceType, assetIds, ExpertEntity.MarkType.EXPERT);
    }

    /**
     * 从资产 ID 列表中过滤掉 Builtin 资产
     *
     * @param resourceType 资源类型
     * @param assetIds     资产 ID 列表
     * @return 排除 Builtin 后的 ID 集合
     */
    public Set<String> filterOutBuiltins(ResourceType resourceType, Collection<String> assetIds) {
        return filterByMarkType(resourceType, assetIds, ExpertEntity.MarkType.BUILTIN);
    }

    // ==================== 派生次数 ====================

    /**
     * 增加 Expert 派生次数
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     */
    public void incrementUsageCount(ResourceType resourceType, String resourceId) {
        getExpertStore().findByResource(resourceType, resourceId)
                .ifPresent(mark -> {
                    getExpertStore().incrementUsageCount(mark.getId());
                    log.debug("[AssetMarkService] Incremented usage count: {} {}", resourceType, resourceId);
                });
    }
}
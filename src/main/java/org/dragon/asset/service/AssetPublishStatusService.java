package org.dragon.asset.service;

import lombok.extern.slf4j.Slf4j;

import org.dragon.asset.enums.PublishStatus;
import org.dragon.asset.store.AssetPublishStatusStore;
import org.dragon.datasource.entity.AssetPublishStatusEntity;
import org.dragon.permission.enums.ResourceType;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * AssetPublishStatusService 资产发布状态服务
 *
 * <p>管理资产的草稿/发布/归档状态，独立于资产表
 *
 * <p>状态流转：
 * <pre>
 * DRAFT → PUBLISHED → ARCHIVED
 * </pre>
 *
 * <p>发布操作需要权限校验，审批通过后才能发布
 */
@Slf4j
@Service
public class AssetPublishStatusService {

    private final AssetPublishStatusStore publishStatusStore;

    /**
     * 资源类型默认发布状态映射
     * <p>
     * - DRAFT: 需要审批才能发布
     * - PUBLISHED: 默认直接发布，无需审批
     * - null: 该资源类型不需要发布状态管理
     */
    private static final Map<ResourceType, PublishStatus> DEFAULT_PUBLISH_STATUS = new EnumMap<>(ResourceType.class);

    static {
        // 需要审批发布的资产（默认为 DRAFT）
        DEFAULT_PUBLISH_STATUS.put(ResourceType.CHARACTER, PublishStatus.DRAFT);
        DEFAULT_PUBLISH_STATUS.put(ResourceType.SKILL, PublishStatus.DRAFT);
        DEFAULT_PUBLISH_STATUS.put(ResourceType.OBSERVER, PublishStatus.DRAFT);
        DEFAULT_PUBLISH_STATUS.put(ResourceType.TRAIT, PublishStatus.DRAFT);
        DEFAULT_PUBLISH_STATUS.put(ResourceType.WORKSPACE, PublishStatus.DRAFT);
        DEFAULT_PUBLISH_STATUS.put(ResourceType.COMMONSENSE, PublishStatus.DRAFT);

        // 默认直接发布的资产
        DEFAULT_PUBLISH_STATUS.put(ResourceType.MODEL, PublishStatus.PUBLISHED);
        DEFAULT_PUBLISH_STATUS.put(ResourceType.TEMPLATE, PublishStatus.PUBLISHED);
    }

    public AssetPublishStatusService(StoreFactory storeFactory) {
        this.publishStatusStore = storeFactory.get(AssetPublishStatusStore.class);
    }

    /**
     * 获取资源类型的默认发布状态
     *
     * @param resourceType 资源类型
     * @return 默认发布状态，如果不需要发布状态管理则返回 null
     */
    public PublishStatus getDefaultStatus(ResourceType resourceType) {
        return DEFAULT_PUBLISH_STATUS.get(resourceType);
    }

    /**
     * 检查资源类型是否需要发布状态管理
     *
     * @param resourceType 资源类型
     * @return 是否需要发布状态管理
     */
    public boolean requiresPublishStatus(ResourceType resourceType) {
        return DEFAULT_PUBLISH_STATUS.containsKey(resourceType)
                && DEFAULT_PUBLISH_STATUS.get(resourceType) != null;
    }

    /**
     * 初始化资产的发布状态（使用资源类型的默认状态）
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param operatorId 操作人 ID
     * @return 创建的发布状态实体，如果不需要发布状态管理则返回 null
     */
    public AssetPublishStatusEntity initializeStatus(ResourceType resourceType, String resourceId, String operatorId) {
        PublishStatus defaultStatus = getDefaultStatus(resourceType);
        if (defaultStatus == null) {
            log.debug("[AssetPublishStatusService] Resource type {} does not require publish status management",
                    resourceType);
            return null;
        }

        if (publishStatusStore.exists(resourceType.name(), resourceId)) {
            throw new IllegalArgumentException("发布状态已存在: " + resourceType + ":" + resourceId);
        }

        LocalDateTime now = LocalDateTime.now();
        AssetPublishStatusEntity entity = AssetPublishStatusEntity.builder()
                .id(UUID.randomUUID().toString())
                .resourceType(resourceType.name())
                .resourceId(resourceId)
                .status(defaultStatus.name())
                .version(1)
                .publishedAt(defaultStatus == PublishStatus.PUBLISHED ? now : null)
                .publishedBy(defaultStatus == PublishStatus.PUBLISHED ? operatorId : null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publishStatusStore.save(entity);
        log.info("[AssetPublishStatusService] Initialized publish status: {}:{} -> {}, operator: {}",
                resourceType, resourceId, defaultStatus, operatorId);

        return entity;
    }

    /**
     * 初始化资产的发布状态（指定状态）
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param operatorId 操作人 ID
     * @param status 初始状态
     * @return 创建的发布状态实体
     */
    public AssetPublishStatusEntity initializeStatus(ResourceType resourceType, String resourceId, String operatorId, PublishStatus status) {
        if (publishStatusStore.exists(resourceType.name(), resourceId)) {
            throw new IllegalArgumentException("发布状态已存在: " + resourceType + ":" + resourceId);
        }

        LocalDateTime now = LocalDateTime.now();
        AssetPublishStatusEntity entity = AssetPublishStatusEntity.builder()
                .id(UUID.randomUUID().toString())
                .resourceType(resourceType.name())
                .resourceId(resourceId)
                .status(status.name())
                .version(1)
                .publishedAt(status == PublishStatus.PUBLISHED ? now : null)
                .publishedBy(status == PublishStatus.PUBLISHED ? operatorId : null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        publishStatusStore.save(entity);
        log.info("[AssetPublishStatusService] Initialized publish status: {}:{} -> {}, operator: {}",
                resourceType, resourceId, status, operatorId);

        return entity;
    }

    /**
     * 获取资产的发布状态
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return Optional 发布状态实体
     */
    public Optional<AssetPublishStatusEntity> getStatus(ResourceType resourceType, String resourceId) {
        return publishStatusStore.findByResource(resourceType.name(), resourceId);
    }

    /**
     * 获取资产的发布状态，如果不存在则返回 DRAFT
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 发布状态
     */
    public PublishStatus getStatusOrDefault(ResourceType resourceType, String resourceId) {
        return publishStatusStore.findByResource(resourceType.name(), resourceId)
                .map(e -> PublishStatus.valueOf(e.getStatus()))
                .orElse(PublishStatus.DRAFT);
    }

    /**
     * 发布资产（审批通过后调用）
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param operatorId 操作人 ID（审批人）
     * @param snapshot 发布时的资产快照（可选）
     */
    public void publish(ResourceType resourceType, String resourceId, String operatorId, Object snapshot) {
        AssetPublishStatusEntity entity = publishStatusStore.findByResource(resourceType.name(), resourceId)
                .orElseGet(() -> initializeStatus(resourceType, resourceId, operatorId));

        if (PublishStatus.PUBLISHED.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("资产已发布: " + resourceType + ":" + resourceId);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(PublishStatus.PUBLISHED.name());
        entity.setVersion(entity.getVersion() + 1);
        entity.setPublishedAt(now);
        entity.setPublishedBy(operatorId);
        entity.setSnapshot(snapshot);
        entity.setUpdatedAt(now);

        publishStatusStore.update(entity);
        log.info("[AssetPublishStatusService] Published asset: {}:{} -> PUBLISHED(v{}), operator: {}",
                resourceType, resourceId, entity.getVersion(), operatorId);
    }

    /**
     * 归档资产
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param operatorId 操作人 ID
     */
    public void archive(ResourceType resourceType, String resourceId, String operatorId) {
        AssetPublishStatusEntity entity = publishStatusStore.findByResource(resourceType.name(), resourceId)
                .orElseThrow(() -> new IllegalArgumentException("发布状态不存在: " + resourceType + ":" + resourceId));

        if (!PublishStatus.PUBLISHED.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("只有已发布的资产才能归档: " + resourceType + ":" + resourceId);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(PublishStatus.ARCHIVED.name());
        entity.setArchivedAt(now);
        entity.setArchivedBy(operatorId);
        entity.setUpdatedAt(now);

        publishStatusStore.update(entity);
        log.info("[AssetPublishStatusService] Archived asset: {}:{} -> ARCHIVED, operator: {}",
                resourceType, resourceId, operatorId);
    }

    /**
     * 恢复归档的资产到已发布状态
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param operatorId 操作人 ID
     */
    public void unarchive(ResourceType resourceType, String resourceId, String operatorId) {
        AssetPublishStatusEntity entity = publishStatusStore.findByResource(resourceType.name(), resourceId)
                .orElseThrow(() -> new IllegalArgumentException("发布状态不存在: " + resourceType + ":" + resourceId));

        if (!PublishStatus.ARCHIVED.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("只有已归档的资产才能恢复: " + resourceType + ":" + resourceId);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(PublishStatus.PUBLISHED.name());
        entity.setArchivedAt(null);
        entity.setArchivedBy(null);
        entity.setUpdatedAt(now);

        publishStatusStore.update(entity);
        log.info("[AssetPublishStatusService] Unarchived asset: {}:{} -> PUBLISHED, operator: {}",
                resourceType, resourceId, operatorId);
    }

    /**
     * 回退到草稿状态
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param operatorId 操作人 ID
     */
    public void revertToDraft(ResourceType resourceType, String resourceId, String operatorId) {
        AssetPublishStatusEntity entity = publishStatusStore.findByResource(resourceType.name(), resourceId)
                .orElseThrow(() -> new IllegalArgumentException("发布状态不存在: " + resourceType + ":" + resourceId));

        if (!PublishStatus.PUBLISHED.name().equals(entity.getStatus())) {
            throw new IllegalArgumentException("只有已发布的资产才能回退到草稿: " + resourceType + ":" + resourceId);
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(PublishStatus.DRAFT.name());
        entity.setUpdatedAt(now);

        publishStatusStore.update(entity);
        log.info("[AssetPublishStatusService] Reverted to draft: {}:{} -> DRAFT, operator: {}",
                resourceType, resourceId, operatorId);
    }

    /**
     * 设置为待审批状态（发布审批提交后调用）
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     */
    public void setPending(ResourceType resourceType, String resourceId) {
        AssetPublishStatusEntity entity = publishStatusStore.findByResource(resourceType.name(), resourceId)
                .orElseThrow(() -> new IllegalArgumentException("发布状态不存在: " + resourceType + ":" + resourceId));

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(PublishStatus.PENDING.name());
        entity.setUpdatedAt(now);

        publishStatusStore.update(entity);
        log.info("[AssetPublishStatusService] Set to pending: {}:{}", resourceType, resourceId);
    }

    /**
     * 从待审批状态回退到草稿（审批撤回或拒绝时调用）
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     */
    public void revertPendingToDraft(ResourceType resourceType, String resourceId) {
        AssetPublishStatusEntity entity = publishStatusStore.findByResource(resourceType.name(), resourceId)
                .orElseThrow(() -> new IllegalArgumentException("发布状态不存在: " + resourceType + ":" + resourceId));

        if (!PublishStatus.PENDING.name().equals(entity.getStatus())) {
            log.warn("[AssetPublishStatusService] Cannot revert non-pending status to draft: {}:{}, current: {}",
                    resourceType, resourceId, entity.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(PublishStatus.DRAFT.name());
        entity.setUpdatedAt(now);

        publishStatusStore.update(entity);
        log.info("[AssetPublishStatusService] Reverted pending to draft: {}:{}", resourceType, resourceId);
    }

    /**
     * 获取资源类型下所有指定状态的资产
     *
     * @param resourceType 资源类型
     * @param status 状态
     * @return 资产 ID 列表
     */
    public List<String> getAssetIdsByStatus(ResourceType resourceType, PublishStatus status) {
        return publishStatusStore.findByResourceTypeAndStatus(resourceType.name(), status.name())
                .stream()
                .map(AssetPublishStatusEntity::getResourceId)
                .toList();
    }

    /**
     * 获取资源类型下所有已发布的资产
     *
     * @param resourceType 资源类型
     * @return 已发布资产 ID 列表
     */
    public List<String> getPublishedAssetIds(ResourceType resourceType) {
        return getAssetIdsByStatus(resourceType, PublishStatus.PUBLISHED);
    }

    /**
     * 检查资产是否已发布
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 是否已发布
     */
    public boolean isPublished(ResourceType resourceType, String resourceId) {
        return publishStatusStore.findByResource(resourceType.name(), resourceId)
                .map(e -> PublishStatus.PUBLISHED.name().equals(e.getStatus()))
                .orElse(false);
    }

    /**
     * 删除资产的发布状态
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     */
    public void deleteStatus(ResourceType resourceType, String resourceId) {
        if (publishStatusStore.exists(resourceType.name(), resourceId)) {
            publishStatusStore.delete(resourceType.name(), resourceId);
            log.info("[AssetPublishStatusService] Deleted publish status: {}:{}", resourceType, resourceId);
        }
    }
}
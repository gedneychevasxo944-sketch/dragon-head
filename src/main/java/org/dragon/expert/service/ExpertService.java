package org.dragon.expert.service;

import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.enums.PublishStatus;
import org.dragon.asset.factory.AssetFactory;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.store.SkillStore;
import org.dragon.store.StoreFactory;
import org.dragon.expert.derive.CopyContext;
import org.dragon.expert.derive.CopyStrategy;
import org.dragon.expert.derive.CopyStrategyFactory;
import org.dragon.expert.derive.CreateContext;
import org.dragon.datasource.entity.ExpertEntity;
import org.dragon.expert.store.ExpertStore;
import org.dragon.trait.store.TraitStore;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ExpertService Expert 模板服务
 *
 * <p>提供 Expert 的标记、查询、派生等业务逻辑。
 * Expert 是资产的 fork 副本，expert_mark 表记录哪些资产是 Expert。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertService {

    private final StoreFactory storeFactory;
    private final CopyStrategyFactory copyStrategyFactory;
    private final AssetPublishStatusService publishStatusService;
    private final AssetMemberService assetMemberService;
    private final AssetAssociationService assetAssociationService;
    private final CharacterRegistry characterRegistry;
    private final SkillStore skillStore;
    private final TraitStore traitStore;
    private final AssetFactory assetFactory;

    private ExpertStore getExpertStore() {
        return storeFactory.get(ExpertStore.class);
    }

    /**
     * 白板创建 Expert（一步完成）
     * 1. 创建空白资产（通过 AssetFactory 设置 owner 和 publishStatus）
     * 2. 标记为 Expert
     *
     * @param request 创建请求
     * @return 创建的资产
     */
    public Object createWithExpert(CreateContext request) {
        Long userId = Long.valueOf(UserUtils.getUserId());

        // 1. 创建空白资产（带 owner 和 publishStatus）
        Object asset;
        String assetId;
        switch (request.getResourceType()) {
            case CHARACTER -> {
                org.dragon.character.Character c = assetFactory.createBlankCharacter(
                        request.getName(), request.getDescription());
                asset = c;
                assetId = c.getId();
            }
            case SKILL -> {
                org.dragon.skill.domain.SkillDO s = assetFactory.createBlankSkill(
                        request.getName(), request.getName(), request.getDescription());
                asset = s;
                assetId = s.getSkillId();
            }
            case TRAIT -> {
                org.dragon.datasource.entity.TraitEntity t = assetFactory.createBlankTrait(
                        request.getName(), request.getDescription(), null);
                asset = t;
                assetId = t.getId();
            }
            default -> throw new UnsupportedOperationException(
                    "Unsupported resource type for Expert: " + request.getResourceType());
        }

        // 2. 创建 Expert 标记
        ExpertEntity mark = ExpertEntity.builder()
                .resourceType(request.getResourceType())
                .resourceId(assetId)
                .category(request.getCategory())
                .preview(request.getPreview())
                .targetAudience(request.getTargetAudience())
                .usageCount(0)
                .build();
        getExpertStore().save(mark);

        // 3. Expert 资产默认 PUBLISHED
        publishStatusService.initializeStatus(
                request.getResourceType(), assetId, String.valueOf(userId), PublishStatus.PUBLISHED);

        log.info("[ExpertService] Created expert with blank asset: {} {}", request.getResourceType(), assetId);

        return asset;
    }

    /**
     * 从已有资产创建 Expert（fork）
     *
     * @param resourceType    资源类型
     * @param resourceId       资源 ID
     * @param category        分类
     * @param preview         预览文本
     * @param targetAudience  目标用户群体
     * @return Expert 标记实体
     */
    public ExpertEntity createExpertFromAsset(ResourceType resourceType, String resourceId,
                                              String category, String preview, String targetAudience) {
        // 1. 检查资产是否存在
        if (!assetExists(resourceType, resourceId)) {
            throw new IllegalArgumentException("Asset not found: " + resourceType + "/" + resourceId);
        }

        // 2. 检查是否已经是 Expert
        if (getExpertStore().findByResource(resourceType, resourceId).isPresent()) {
            throw new IllegalStateException("Asset is already an Expert");
        }

        // 3. 获取源资产
        Object sourceAsset = findAsset(resourceType, resourceId);

        // 4. 使用 CopyStrategy 全量复制
        Long operatorId = Long.valueOf(UserUtils.getUserId());
        CopyContext copyContext = CopyContext.builder()
                .sourceAsset(sourceAsset)
                .operatorId(operatorId)
                .markAsExpert(true)
                .category(category)
                .preview(preview)
                .targetAudience(targetAudience)
                .build();

        CopyStrategy strategy = copyStrategyFactory.getStrategy(resourceType);
        Object copiedAsset = strategy.copy(sourceAsset, copyContext);
        String copiedAssetId = getAssetId(copiedAsset);

        // 5. 创建 Expert 标记
        ExpertEntity mark = ExpertEntity.builder()
                .resourceType(resourceType)
                .resourceId(copiedAssetId)
                .category(category)
                .preview(preview)
                .targetAudience(targetAudience)
                .usageCount(0)
                .build();
        getExpertStore().save(mark);

        // 6. 为副本资产创建 owner 和 publishStatus
        assetMemberService.addOwnerDirectly(resourceType, copiedAssetId, operatorId);
        publishStatusService.initializeStatus(resourceType, copiedAssetId, String.valueOf(operatorId), PublishStatus.PUBLISHED);

        // 7. 建立 fork 关联 (source -> expert)
        assetAssociationService.createAssociation(
                AssociationType.EXPERT_SOURCE,
                resourceType, resourceId,
                resourceType, copiedAssetId
        );

        log.info("[ExpertService] Created expert from asset: {} {} -> expert {} {}",
                resourceType, resourceId, resourceType, copiedAssetId);
        return mark;
    }

    /**
     * 从 Expert 派生创建新资产（当模板使用）
     *
     * @param expertResourceType Expert 资源类型
     * @param expertResourceId   Expert 资源 ID
     * @return 创建的新资产
     */
    public Object deriveFromExpert(ResourceType expertResourceType, String expertResourceId) {
        // 1. 获取 Expert 标记
        ExpertEntity mark = getExpertStore().findByResource(expertResourceType, expertResourceId)
                .orElseThrow(() -> new IllegalArgumentException("Expert not found: " + expertResourceType + "/" + expertResourceId));

        // 2. 获取 Expert 资产
        Object expertAsset = findAsset(expertResourceType, expertResourceId);
        if (expertAsset == null) {
            throw new IllegalArgumentException("Expert asset not found: " + expertResourceType + "/" + expertResourceId);
        }

        // 3. 使用 CopyStrategy 全量复制（不标记为 Expert）
        Long operatorId = Long.valueOf(UserUtils.getUserId());
        CopyContext copyContext = CopyContext.builder()
                .sourceAsset(expertAsset)
                .operatorId(operatorId)
                .markAsExpert(false)
                .build();

        CopyStrategy strategy = copyStrategyFactory.getStrategy(expertResourceType);
        Object newAsset = strategy.copy(expertAsset, copyContext);
        String newAssetId = getAssetId(newAsset);

        // 4. 为新资产创建 owner 和 publishStatus
        assetMemberService.addOwnerDirectly(expertResourceType, newAssetId, operatorId);
        publishStatusService.initializeStatus(expertResourceType, newAssetId, String.valueOf(operatorId));

        // 5. 增加 Expert 派生次数
        mark.setUsageCount(mark.getUsageCount() + 1);
        getExpertStore().update(mark);

        // 6. 建立派生关联 (expert -> derived)
        assetAssociationService.createAssociation(
                AssociationType.EXPERT_SOURCE,
                expertResourceType, expertResourceId,
                expertResourceType, newAssetId
        );

        log.info("[ExpertService] Derived asset from expert: {} {} -> new {} {}",
                expertResourceType, expertResourceId, expertResourceType, newAssetId);

        return newAsset;
    }

    /**
     * 取消 Expert 标记
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     */
    public void unmarkExpert(ResourceType resourceType, String resourceId) {
        getExpertStore().deleteByResource(resourceType, resourceId);
        log.info("[ExpertService] Unmarked expert: {} {}", resourceType, resourceId);
    }

    /**
     * 查询某资产是否是 Expert
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 是否是 Expert
     */
    public boolean isExpert(ResourceType resourceType, String resourceId) {
        return getExpertStore().findByResource(resourceType, resourceId).isPresent();
    }

    /**
     * 获取 Expert 标记信息
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return Expert 标记实体
     */
    public Optional<ExpertEntity> getExpertMark(ResourceType resourceType, String resourceId) {
        return getExpertStore().findByResource(resourceType, resourceId);
    }

    /**
     * 获取 Expert 列表
     *
     * @param resourceType 资源类型（null 表示全部）
     * @param category     分类（null 表示全部）
     * @return Expert 标记列表
     */
    public List<ExpertEntity> listExperts(ResourceType resourceType, String category) {
        List<ExpertEntity> marks;
        if (resourceType != null) {
            marks = getExpertStore().findByResourceType(resourceType);
        } else {
            marks = getExpertStore().findAll();
        }

        if (category != null && !category.isBlank()) {
            marks = marks.stream()
                    .filter(m -> category.equals(m.getCategory()))
                    .toList();
        }
        return marks;
    }

    /**
     * 获取 Expert 资产对象
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 资产对象
     */
    public Object getExpertAsset(ResourceType resourceType, String resourceId) {
        return findAsset(resourceType, resourceId);
    }

    /**
     * 从资产ID列表中过滤掉已 Expert 标记的资产
     *
     * @param resourceType 资源类型
     * @param assetIds    资产ID列表
     * @return 排除 Expert 后的资产ID集合
     */
    public Set<String> filterOutExpertMarked(ResourceType resourceType, java.util.Collection<String> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> expertIds = getExpertStore().findByResourceType(resourceType)
                .stream()
                .map(ExpertEntity::getResourceId)
                .collect(Collectors.toSet());
        return assetIds.stream()
                .filter(id -> !expertIds.contains(id))
                .collect(Collectors.toSet());
    }

    // ========== 内部方法 ==========

    private boolean assetExists(ResourceType rt, String rid) {
        return switch (rt) {
            case CHARACTER -> characterRegistry.exists(rid);
            case SKILL -> skillStore.existsBySkillId(rid);
            case TRAIT -> traitStore.findById(rid).isPresent();
            default -> throw new UnsupportedOperationException("Unsupported resource type: " + rt);
        };
    }

    private Object findAsset(ResourceType rt, String rid) {
        return switch (rt) {
            case CHARACTER -> characterRegistry.get(rid).orElse(null);
            case SKILL -> skillStore.findLatestBySkillId(rid).orElse(null);
            case TRAIT -> traitStore.findById(rid).orElse(null);
            default -> throw new UnsupportedOperationException("Unsupported resource type: " + rt);
        };
    }

    private String getAssetId(Object asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset is null");
        }
        if (asset instanceof Character character) {
            return character.getId();
        }
        // 其他类型暂时未实现
        throw new UnsupportedOperationException("Unsupported asset type: " + asset.getClass().getName());
    }
}
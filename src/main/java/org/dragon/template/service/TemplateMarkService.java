package org.dragon.template.service;

import org.dragon.asset.enums.AssociationType;
import org.dragon.asset.enums.PublishStatus;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.store.SkillStore;
import org.dragon.store.StoreFactory;
import org.dragon.template.derive.CreateContext;
import org.dragon.template.derive.DeriveContext;
import org.dragon.template.derive.DeriveStrategy;
import org.dragon.template.derive.DeriveStrategyFactory;
import org.dragon.template.derive.DeriveTemplateRequest;
import org.dragon.datasource.entity.TemplateMarkEntity;
import org.dragon.template.store.TemplateMarkStore;
import org.dragon.trait.store.TraitStore;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

/**
 * TemplateMarkService 模板标记服务
 *
 * <p>提供模板的标记、查询、派生等业务逻辑。
 *
 * @author yijunw
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateMarkService {

    private final StoreFactory storeFactory;
    private final DeriveStrategyFactory deriveStrategyFactory;
    private final AssetPublishStatusService publishStatusService;
    private final AssetAssociationService assetAssociationService;
    private final CharacterRegistry characterRegistry;
    private final SkillStore skillStore;
    private final TraitStore traitStore;

    private TemplateMarkStore getTemplateMarkStore() {
        return storeFactory.get(TemplateMarkStore.class);
    }

    /**
     * 白板创建模板（一步完成）
     * 1. 创建草稿资产
     * 2. 标记为模板
     *
     * @param request 创建模板请求
     * @return 创建的资产
     */
    public Object createWithTemplate(CreateContext request) {
        // 1. 获取对应类型的 DeriveStrategy
        DeriveStrategy strategy = deriveStrategyFactory.getStrategy(request.getResourceType());

        // 2. 使用策略创建草稿资产
        Object asset = strategy.createDraft(request);

        // 3. 获取资产 ID
        String assetId = getAssetId(asset);

        // 4. 创建模板标记
        TemplateMarkEntity mark = TemplateMarkEntity.builder()
                .resourceType(request.getResourceType())
                .resourceId(assetId)
                .category(request.getCategory())
                .preview(request.getPreview())
                .targetAudience(request.getTargetAudience())
                .usageCount(0)
                .build();
        getTemplateMarkStore().save(mark);

        // 5. 模板资产默认 PUBLISHED
        Long userId = Long.valueOf(UserUtils.getUserId());
        publishStatusService.initializeStatus(
                request.getResourceType(), assetId, String.valueOf(userId), PublishStatus.PUBLISHED);

        log.info("[TemplateMarkService] Created template with draft asset: {} {}", request.getResourceType(), assetId);

        return asset;
    }

    /**
     * 将已有资产标记为模板
     *
     * @param resourceType    资源类型
     * @param resourceId      资源 ID
     * @param category        分类
     * @param preview         预览文本
     * @param targetAudience  目标用户群体
     * @return 模板标记实体
     */
    public TemplateMarkEntity markAsTemplate(ResourceType resourceType, String resourceId,
                                            String category, String preview, String targetAudience) {
        // 1. 检查资产是否存在
        if (!assetExists(resourceType, resourceId)) {
            throw new IllegalArgumentException("Asset not found: " + resourceType + "/" + resourceId);
        }

        // 2. 检查是否已经是模板
        if (getTemplateMarkStore().findByResource(resourceType, resourceId).isPresent()) {
            throw new IllegalStateException("Asset is already a template");
        }

        // 3. 创建标记
        TemplateMarkEntity mark = TemplateMarkEntity.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .category(category)
                .preview(preview)
                .targetAudience(targetAudience)
                .usageCount(0)
                .build();
        getTemplateMarkStore().save(mark);

        // 4. 模板资产默认 PUBLISHED
        Long userId = Long.valueOf(UserUtils.getUserId());
        publishStatusService.initializeStatus(resourceType, resourceId, String.valueOf(userId), PublishStatus.PUBLISHED);

        log.info("[TemplateMarkService] Marked as template: {} {}", resourceType, resourceId);
        return mark;
    }

    /**
     * 取消模板标记
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     */
    public void unmarkTemplate(ResourceType resourceType, String resourceId) {
        getTemplateMarkStore().deleteByResource(resourceType, resourceId);
        log.info("[TemplateMarkService] Unmarked template: {} {}", resourceType, resourceId);
    }

    /**
     * 查询某资产是否是模板
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 是否是模板
     */
    public boolean isTemplate(ResourceType resourceType, String resourceId) {
        return getTemplateMarkStore().findByResource(resourceType, resourceId).isPresent();
    }

    /**
     * 获取模板标记信息
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 模板标记实体
     */
    public Optional<TemplateMarkEntity> getTemplateMark(ResourceType resourceType, String resourceId) {
        return getTemplateMarkStore().findByResource(resourceType, resourceId);
    }

    /**
     * 获取模板列表
     *
     * @param resourceType 资源类型（null 表示全部）
     * @param category     分类（null 表示全部）
     * @return 模板标记列表
     */
    public List<TemplateMarkEntity> listTemplates(ResourceType resourceType, String category) {
        List<TemplateMarkEntity> marks;
        if (resourceType != null) {
            marks = getTemplateMarkStore().findByResourceType(resourceType);
        } else {
            marks = getTemplateMarkStore().findAll();
        }

        if (category != null && !category.isBlank()) {
            marks = marks.stream()
                    .filter(m -> category.equals(m.getCategory()))
                    .toList();
        }
        return marks;
    }

    /**
     * 从模板派生创建新资产
     *
     * @param resourceType       资源类型
     * @param templateResourceId 模板资源 ID
     * @param request           派生请求
     * @return 创建的新资产
     */
    public Object derive(ResourceType resourceType, String templateResourceId, DeriveTemplateRequest request) {
        // 1. 获取模板标记
        TemplateMarkEntity mark = getTemplateMarkStore().findByResource(resourceType, templateResourceId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + resourceType + "/" + templateResourceId));

        // 2. 获取模板资产
        Object templateAsset = findAsset(resourceType, templateResourceId);

        // 3. 使用 DeriveStrategy 派生
        DeriveStrategy strategy = deriveStrategyFactory.getStrategy(resourceType);
        DeriveContext context = DeriveContext.builder()
                .templateAsset(templateAsset)
                .request(request)
                .operatorId(Long.valueOf(UserUtils.getUserId()))
                .build();
        Object newAsset = strategy.derive(templateAsset, context);

        // 4. 获取新资产 ID
        String newAssetId = getAssetId(newAsset);

        // 5. 增加派生次数
        mark.setUsageCount(mark.getUsageCount() + 1);
        getTemplateMarkStore().update(mark);

        // 6. 建立派生关联
        assetAssociationService.createAssociation(
                AssociationType.TEMPLATE_ASSET,
                resourceType, templateResourceId,
                resourceType, newAssetId
        );

        log.info("[TemplateMarkService] Derived asset from template: {} {} -> new {}",
                resourceType, templateResourceId, newAssetId);

        return newAsset;
    }

    /**
     * 获取模板资产对象
     *
     * @param resourceType 资源类型
     * @param resourceId   资源 ID
     * @return 资产对象
     */
    public Object getTemplateAsset(ResourceType resourceType, String resourceId) {
        return findAsset(resourceType, resourceId);
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

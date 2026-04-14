package org.dragon.expert.derive;

import org.dragon.asset.factory.AssetFactory;
import org.dragon.permission.enums.ResourceType;
import org.dragon.trait.store.TraitStore;
import org.dragon.datasource.entity.TraitEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * TraitCopyStrategy Trait 复制策略
 *
 * <p>委托 AssetFactory.copyTrait() 实现复制。
 *
 * @author yijunw
 */
@Slf4j
@Component
public class TraitCopyStrategy implements CopyStrategy {

    private final AssetFactory assetFactory;
    private final TraitStore traitStore;

    public TraitCopyStrategy(AssetFactory assetFactory, TraitStore traitStore) {
        this.assetFactory = assetFactory;
        this.traitStore = traitStore;
    }

    @Override
    public ResourceType getSupportedType() {
        return ResourceType.TRAIT;
    }

    @Override
    public Object copy(Object sourceAsset, CopyContext context) {
        if (sourceAsset == null) {
            throw new UnsupportedOperationException("Use ExpertService.createWithExpert for white-board creation");
        }

        TraitEntity source = (TraitEntity) sourceAsset;
        String copiedTraitId = assetFactory.copyTrait(source.getId(), context.getOperatorId());

        return traitStore.findById(copiedTraitId).orElse(null);
    }
}
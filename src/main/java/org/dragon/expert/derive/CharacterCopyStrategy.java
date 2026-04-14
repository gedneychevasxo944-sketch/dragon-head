package org.dragon.expert.derive;

import org.dragon.asset.factory.AssetFactory;
import org.dragon.character.Character;
import org.dragon.permission.enums.ResourceType;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * CharacterCopyStrategy Character 复制策略
 *
 * <p>委托 AssetFactory.copyCharacter() 实现深度复制。
 *
 * @author yijunw
 */
@Slf4j
@Component
public class CharacterCopyStrategy implements CopyStrategy {

    private final AssetFactory assetFactory;

    public CharacterCopyStrategy(AssetFactory assetFactory) {
        this.assetFactory = assetFactory;
    }

    @Override
    public ResourceType getSupportedType() {
        return ResourceType.CHARACTER;
    }

    @Override
    public Object copy(Object sourceAsset, CopyContext context) {
        if (sourceAsset == null) {
            throw new UnsupportedOperationException("Use ExpertService.createWithExpert for white-board creation");
        }

        Character source = (Character) sourceAsset;
        return assetFactory.copyCharacter(source, context.getOperatorId());
    }
}
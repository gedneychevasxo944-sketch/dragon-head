package org.dragon.character.mind;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.datasource.entity.TraitEntity;
import org.dragon.store.StoreFactory;
import org.dragon.trait.store.TraitStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Trait 内容解析服务
 * 将 Trait ID 列表解析为 PersonalityDescriptor.TraitContent 列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraitResolutionService {

    private final StoreFactory storeFactory;

    private TraitStore getStore() {
        return storeFactory.get(TraitStore.class);
    }

    /**
     * 解析 trait IDs → TraitContent 列表
     */
    public List<PersonalityDescriptor.TraitContent> resolveTraits(List<String> traitIds) {
        if (traitIds == null || traitIds.isEmpty()) {
            return List.of();
        }
        return traitIds.stream()
                .map(this::resolveSingle)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PersonalityDescriptor.TraitContent> resolveSingle(String traitId) {
        try {
            return getStore().findById(traitId)
                    .filter(TraitEntity::getEnabled)
                    .map(this::toTraitContent);
        } catch (Exception e) {
            log.warn("[TraitResolutionService] Failed to resolve trait: {}", traitId, e);
            return Optional.empty();
        }
    }

    private PersonalityDescriptor.TraitContent toTraitContent(TraitEntity entity) {
        return PersonalityDescriptor.TraitContent.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .content(entity.getContent())
                .build();
    }
}

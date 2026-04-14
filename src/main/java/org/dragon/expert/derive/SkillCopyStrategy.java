package org.dragon.expert.derive;

import org.dragon.asset.factory.AssetFactory;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.store.SkillStore;
import org.springframework.stereotype.Component;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * SkillCopyStrategy Skill 复制策略
 *
 * <p>委托 AssetFactory.copySkill() 实现复制。
 *
 * @author yijunw
 */
@Slf4j
@Component
public class SkillCopyStrategy implements CopyStrategy {

    private final AssetFactory assetFactory;
    private final SkillStore skillStore;
    private final AssetAssociationService assetAssociationService;

    public SkillCopyStrategy(
            AssetFactory assetFactory,
            SkillStore skillStore,
            AssetAssociationService assetAssociationService) {
        this.assetFactory = assetFactory;
        this.skillStore = skillStore;
        this.assetAssociationService = assetAssociationService;
    }

    @Override
    public ResourceType getSupportedType() {
        return ResourceType.SKILL;
    }

    @Override
    public Object copy(Object sourceAsset, CopyContext context) {
        if (sourceAsset == null) {
            throw new UnsupportedOperationException("Use ExpertService.createWithExpert for white-board creation");
        }

        SkillDO source = (SkillDO) sourceAsset;

        // 1. 复制 Skill 实体
        String copiedSkillId = assetFactory.copySkill(source.getSkillId(), context.getOperatorId());

        // 2. 复制关联的 Tool（TOOL_SKILL）
        // TODO: Tool 复制逻辑 - 需要实现 ToolCopyStrategy 并建立新的关联
        List<String> toolIds = assetAssociationService.getToolsForSkill(source.getSkillId());
        for (String toolId : toolIds) {
            log.info("[SkillCopyStrategy] Tool copy not yet implemented: {}", toolId);
        }

        return skillStore.findLatestBySkillId(copiedSkillId).orElse(null);
    }
}
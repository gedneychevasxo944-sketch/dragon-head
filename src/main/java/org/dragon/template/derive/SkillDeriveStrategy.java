package org.dragon.template.derive;

import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.service.SkillRegisterService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SkillDeriveStrategy Skill 派生策略
 *
 * @author yijunw
 */
@Component
public class SkillDeriveStrategy implements DeriveStrategy {

    private final SkillRegisterService skillRegisterService;

    public SkillDeriveStrategy(SkillRegisterService skillRegisterService) {
        this.skillRegisterService = skillRegisterService;
    }

    @Override
    public ResourceType getSupportedType() {
        return ResourceType.SKILL;
    }

    @Override
    public Object derive(Object templateAsset, DeriveContext context) {
        SkillDO template = (SkillDO) templateAsset;
        DeriveTemplateRequest request = context.getRequest();

        // Skill 的派生逻辑较复杂，需要通过 SkillRegisterService
        // 这里简化处理，实际派生可能需要更多参数
        throw new UnsupportedOperationException("Skill derive not yet implemented, use createDraft instead");
    }

    @Override
    public Object createDraft(CreateContext request) {
        // Skill 的创建需要完整的元数据，这里简化处理
        // 实际实现可能需要调用 SkillRegisterService
        throw new UnsupportedOperationException("Skill createDraft not yet implemented");
    }
}

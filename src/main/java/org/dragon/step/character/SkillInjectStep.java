package org.dragon.step.character;

import java.util.List;

import org.dragon.agent.react.ReActContext;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.step.StepResult;

import lombok.extern.slf4j.Slf4j;

/**
 * SkillInjectStep - Skill 能力列表注入
 *
 * <p>在每次 ReAct 迭代的 Think 之前执行。
 * 加载当前 Character 在当前 Workspace 下可用的 Skill 列表，
 * 注入到上下文供 LLM 了解自己有哪些可用工具/能力。
 *
 * <p>Skill 是 Character 的能力单元，比如"发送邮件"、"查天气"等。
 * LLM 知道有哪些 Skill 后，才能决定调用哪个。
 *
 * @author yijunw
 */
@Slf4j
public class SkillInjectStep extends CharacterStep {

    private final SkillRegistry skillRegistry;

    public SkillInjectStep(SkillRegistry skillRegistry) {
        super("skillInject");
        this.skillRegistry = skillRegistry;
    }

    public SkillInjectStep() {
        super("skillInject");
        this.skillRegistry = null;
    }

    @Override
    protected StepResult doExecute(ReActContext ctx) throws Exception {
        if (skillRegistry == null) {
            return StepResult.success(getName(), "skillRegistry_not_available");
        }

        String characterId = ctx.getCharacterId();
        String workspaceId = ctx.getWorkspaceId();
        if (characterId == null) {
            return StepResult.success(getName(), "no_character_id");
        }

        List<SkillDefinition> skills = skillRegistry.getSkills(characterId, workspaceId);
        ctx.setActiveSkills(skills);

        log.debug("[SkillInjectStep] [{}] Loaded {} skills for character {} in workspace {}",
                ctx.getCurrentIteration(), skills.size(), characterId, workspaceId);

        return StepResult.success(getName(), skills);
    }
}
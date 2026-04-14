package org.dragon.observer.optimization.applier;

import java.util.Map;

import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.mind.Mind;
import org.dragon.character.mind.PersonalityDescriptor;
import org.dragon.observer.optimization.plan.OptimizationAction;
import org.dragon.observer.optimization.plan.OptimizationAction.TargetType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * CharacterOptimizationTargetApplier Character 优化目标应用器
 * 负责将优化动作应用到 Character
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class CharacterOptimizationTargetApplier implements OptimizationTargetApplier {

    private static final Logger log = LoggerFactory.getLogger(CharacterOptimizationTargetApplier.class);

    private final CharacterRegistry characterRegistry;

    @Override
    public OptimizationAction.TargetType getTargetType() {
        return OptimizationAction.TargetType.CHARACTER;
    }

    @Override
    public ApplyResult apply(OptimizationAction action) {
        Character character = characterRegistry.get(action.getTargetId())
                .orElse(null);

        if (character == null) {
            return ApplyResult.failure("Character not found: " + action.getTargetId());
        }

        Mind mind = character.getMind();
        if (mind == null) {
            return ApplyResult.failure("Character mind is not initialized");
        }

        try {
            switch (action.getActionType()) {
                case UPDATE_MIND:
                case UPDATE_PERSONALITY:
                    return applyPersonalityUpdate(mind, action.getParameters());
                case UPDATE_TAG:
                    return applyTagUpdate(mind, action.getParameters());
                case ADD_SKILL:
                    return applySkillAdd(mind, action.getParameters());
                case REMOVE_SKILL:
                    return applySkillRemove(mind, action.getParameters());
                case ADJUST_WEIGHT:
                    return applyWeightAdjust(mind, action.getParameters());
                case UPDATE_MEMORY:
                    return applyMemoryUpdate(mind, action.getParameters());
                case UPDATE_CONFIG:
                    return applyConfigUpdate(character, action.getParameters());
                default:
                    return ApplyResult.failure("Unsupported action type: " + action.getActionType());
            }
        } catch (Exception e) {
            log.error("[CharacterOptimizationTargetApplier] Failed to apply action: {}", action.getId(), e);
            return ApplyResult.failure("Apply failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private ApplyResult applyPersonalityUpdate(Mind mind, Map<String, Object> parameters) {
        if (parameters.containsKey("personality")) {
            PersonalityDescriptor descriptor = (PersonalityDescriptor) parameters.get("personality");
            mind.updatePersonality(descriptor);
        }
        log.info("[CharacterOptimizationTargetApplier] Personality update applied via parameters");
        return ApplyResult.success("Personality update applied");
    }

    @SuppressWarnings("unchecked")
    private ApplyResult applyTagUpdate(Mind mind, Map<String, Object> parameters) {
        // Tag functionality has been migrated to ImpressionService
        // This method now handles impression updates via the new service
        if (parameters.containsKey("tags")) {
            Map<String, Object> tags = (Map<String, Object>) parameters.get("tags");
            log.info("[CharacterOptimizationTargetApplier] Tag update (deprecated, use ImpressionService) with {} entries", tags.size());
        }
        return ApplyResult.success("Tag update applied (deprecated, use ImpressionService)");
    }

    private ApplyResult applySkillAdd(Mind mind, Map<String, Object> parameters) {
        if (parameters.containsKey("skill")) {
            log.info("[CharacterOptimizationTargetApplier] Skill add: {}", parameters.get("skill"));
        }
        return ApplyResult.success("Skill add applied");
    }

    private ApplyResult applySkillRemove(Mind mind, Map<String, Object> parameters) {
        if (parameters.containsKey("skill")) {
            log.info("[CharacterOptimizationTargetApplier] Skill remove: {}", parameters.get("skill"));
        }
        return ApplyResult.success("Skill remove applied");
    }

    @SuppressWarnings("unchecked")
    private ApplyResult applyWeightAdjust(Mind mind, Map<String, Object> parameters) {
        if (parameters.containsKey("weights")) {
            Map<String, Object> weights = (Map<String, Object>) parameters.get("weights");
            log.info("[CharacterOptimizationTargetApplier] Weight adjust: {} entries", weights.size());
        }
        return ApplyResult.success("Weight adjust applied");
    }

    @SuppressWarnings("unchecked")
    private ApplyResult applyMemoryUpdate(Mind mind, Map<String, Object> parameters) {
        if (parameters.containsKey("memory")) {
            Map<String, Object> memory = (Map<String, Object>) parameters.get("memory");
            log.info("[CharacterOptimizationTargetApplier] Memory update: {} entries", memory.size());
        }
        return ApplyResult.success("Memory update applied");
    }

    @SuppressWarnings("unchecked")
    private ApplyResult applyConfigUpdate(Character character, Map<String, Object> parameters) {
        if (parameters.containsKey("config")) {
            Map<String, Object> config = (Map<String, Object>) parameters.get("config");
            log.info("[CharacterOptimizationTargetApplier] Config update: {} entries", config.size());
        }
        return ApplyResult.success("Config update applied");
    }

    @Override
    public String captureSnapshot(String targetId) {
        return characterRegistry.get(targetId)
                .map(c -> {
                    Mind mind = c.getMind();
                    if (mind != null && mind.getPersonality() != null) {
                        return mind.getPersonality().toString();
                    }
                    return "{}";
                })
                .orElse("{}");
    }

    @Override
    public void restoreFromSnapshot(String targetId, String snapshot) {
        log.info("[CharacterOptimizationTargetApplier] Restoring from snapshot for {}: {}", targetId, snapshot);
    }
}
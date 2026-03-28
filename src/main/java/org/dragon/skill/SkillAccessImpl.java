package org.dragon.skill;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.model.Skill;
import org.dragon.skill.model.SkillMetadata;
import org.dragon.skill.registry.SkillRuntimeEntry;
import org.dragon.skill.registry.SkillRegistry;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SkillAccess 实现类。
 * 提供对 Character 私有技能的统一访问。
 *
 * @since 1.0
 */
@Slf4j
public class SkillAccessImpl implements SkillAccess {

    private final String characterId;
    private final Long workspaceId;
    private final SkillRegistry skillRegistry;

    public SkillAccessImpl(String characterId, Long workspaceId, SkillRegistry skillRegistry) {
        this.characterId = characterId;
        this.workspaceId = workspaceId;
        this.skillRegistry = skillRegistry;
    }

    @Override
    public String getCharacterId() {
        return characterId;
    }

    @Override
    public Skill get(String skillId) {
        return findAll().stream()
                .filter(s -> s.getId() != null && s.getId().toString().equals(skillId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Skill> findByName(String name) {
        return findAll().stream()
                .filter(s -> s.getName() != null && s.getName().equals(name))
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findByDescription(String description) {
        return findAll().stream()
                .filter(s -> s.getDescription() != null && s.getDescription().contains(description))
                .collect(Collectors.toList());
    }

    @Override
    public SkillMetadata getMetadata(String skillId) {
        return skillRegistry.findAllActiveByCharacter(characterId, workspaceId).stream()
                .filter(e -> e.getSkillEntry().getSkill().getId() != null
                        && e.getSkillEntry().getSkill().getId().toString().equals(skillId))
                .findFirst()
                .map(e -> e.getSkillEntry().getMetadata())
                .orElse(null);
    }

    @Override
    public List<Skill> findByIds(List<String> skillIds) {
        return findAll().stream()
                .filter(s -> s.getId() != null && skillIds.contains(s.getId().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findAll() {
        Collection<SkillRuntimeEntry> entries = skillRegistry.findAllActiveByCharacter(characterId, workspaceId);
        return entries.stream()
                .map(SkillRuntimeEntry::getSkillEntry)
                .map(entry -> entry.getSkill())
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findByCategory(String category) {
        // SkillEntity 有 category 字段，但 model.Skill 没有
        // 暂时返回空列表
        return List.of();
    }

    @Override
    public List<Skill> findByTags(List<String> tags) {
        // model.Skill 没有 tags 字段
        // 暂时返回空列表
        return List.of();
    }

    @Override
    public void register(Skill skill) {
        log.warn("[SkillAccess] register() not implemented for character-level skills");
    }

    @Override
    public void registerBatch(List<Skill> skills) {
        log.warn("[SkillAccess] registerBatch() not implemented for character-level skills");
    }

    @Override
    public void update(Skill skill) {
        log.warn("[SkillAccess] update() not implemented for character-level skills");
    }

    @Override
    public void delete(String skillId) {
        log.warn("[SkillAccess] delete() not implemented for character-level skills");
    }

    @Override
    public void clear() {
        log.warn("[SkillAccess] clear() not implemented for character-level skills");
    }

    @Override
    public int count() {
        return findAll().size();
    }
}
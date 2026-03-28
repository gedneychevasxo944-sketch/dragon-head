package org.dragon.skill;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.model.SkillEntry;
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
                .filter(s -> s.getId().equals(skillId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Skill> findByName(String name) {
        return findAll().stream()
                .filter(s -> s.getName().equals(name))
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findByDescription(String description) {
        return findAll().stream()
                .filter(s -> s.getDescription() != null && s.getDescription().contains(description))
                .collect(Collectors.toList());
    }

    @Override
    public Skill.SkillMetadata getMetadata(String skillId) {
        return findAll().stream()
                .filter(s -> s.getId().equals(skillId))
                .findFirst()
                .map(Skill::getMetadata)
                .orElse(null);
    }

    @Override
    public List<Skill> findByIds(List<String> skillIds) {
        return findAll().stream()
                .filter(s -> skillIds.contains(s.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findAll() {
        Collection<SkillRuntimeEntry> entries = skillRegistry.findAllActiveByCharacter(characterId, workspaceId);
        return entries.stream()
                .map(SkillRuntimeEntry::getSkillEntry)
                .map(this::toAccessSkill)
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findByCategory(String category) {
        return findAll().stream()
                .filter(s -> category.equals(s.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Skill> findByTags(List<String> tags) {
        return findAll().stream()
                .filter(s -> s.getTags() != null && s.getTags().containsAll(tags))
                .collect(Collectors.toList());
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

    /**
     * 将运行时 SkillEntry 转换为 SkillAccess 使用的 Skill。
     */
    private Skill toAccessSkill(SkillEntry entry) {
        org.dragon.skill.model.Skill model = entry.getSkill();
        SkillMetadata modelMetadata = entry.getMetadata();

        Skill.SkillMetadata metadata = null;
        if (modelMetadata != null) {
            metadata = Skill.SkillMetadata.builder()
                    .inputParams(modelMetadata.getRequires() != null
                            ? modelMetadata.getRequires().getParams() != null
                                    ? modelMetadata.getRequires().getParams().stream()
                                            .map(p -> Skill.Parameter.builder()
                                                    .name(p.getName())
                                                    .type(p.getType())
                                                    .description(p.getDescription())
                                                    .required(p.getRequired())
                                                    .defaultValue(p.getDefaultValue())
                                                    .build())
                                            .collect(Collectors.toList())
                                    : null
                            : null)
                    .outputParams(null)
                    .config(null)
                    .build();
        }

        return Skill.builder()
                .id(model.getId() != null ? model.getId().toString() : null)
                .name(model.getName())
                .description(model.getDescription())
                .category(null) // model.Skill doesn't have category
                .tags(null)     // model.Skill doesn't have tags
                .metadata(metadata)
                .build();
    }
}
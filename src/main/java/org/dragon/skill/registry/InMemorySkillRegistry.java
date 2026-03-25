package org.dragon.skill.registry;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.model.Skill;
import org.dragon.skill.model.SkillEntry;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于内存的 Skill 注册表实现。
 *
 * @since 1.0
 */
@Slf4j
@Component
public class InMemorySkillRegistry implements SkillRegistry {

    /** 主索引：name -> SkillEntry */
    private final ConcurrentHashMap<String, SkillEntry> nameIndex = new ConcurrentHashMap<>();

    /** ID 索引：id -> skillName */
    private final ConcurrentHashMap<Long, String> idIndex = new ConcurrentHashMap<>();

    @Override
    public void register(SkillEntry entry) {
        Skill skill = entry.getSkill();
        String name = skill.getName();
        long id = skill.getId();

        // 若已存在旧记录，先清理 ID 索引
        SkillEntry existing = nameIndex.get(name);
        if (existing != null) {
            idIndex.remove(existing.getSkill().getId());
        }

        nameIndex.put(name, entry);
        idIndex.put(id, name);

        log.info("Skill 已注册到运行时注册表: name={}, id={}, version={}",
                name, id, skill.getId());
    }

    @Override
    public void unregister(String skillName) {
        SkillEntry removed = nameIndex.remove(skillName);
        if (removed != null) {
            idIndex.remove(removed.getSkill().getId());
            log.info("Skill 已从运行时注册表注销: name={}", skillName);
        }
    }

    @Override
    public Optional<SkillEntry> findByName(String skillName) {
        return Optional.ofNullable(nameIndex.get(skillName));
    }

    @Override
    public Optional<SkillEntry> findById(long skillId) {
        String name = idIndex.get(skillId);
        if (name == null) return Optional.empty();
        return Optional.ofNullable(nameIndex.get(name));
    }

    @Override
    public Collection<SkillEntry> findAll() {
        return Collections.unmodifiableCollection(nameIndex.values());
    }

    @Override
    public String buildSystemPromptFragment() {
        if (nameIndex.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Available Skills\n\n");
        sb.append("You have access to the following skills. ");
        sb.append("Use them when appropriate based on the user's request.\n\n");

        nameIndex.values().stream()
                .sorted(Comparator.comparing(e -> e.getSkill().getName()))
                .forEach(entry -> {
                    Skill skill = entry.getSkill();
                    sb.append("### ").append(skill.getName()).append("\n\n");
                    sb.append("**Description**: ").append(skill.getDescription()).append("\n\n");
                    if (skill.getContent() != null && !skill.getContent().isBlank()) {
                        sb.append(skill.getContent()).append("\n\n");
                    }
                    sb.append("---\n\n");
                });

        return sb.toString();
    }

    @Override
    public void clear() {
        nameIndex.clear();
        idIndex.clear();
        log.info("Skill 运行时注册表已清空");
    }

    @Override
    public int size() {
        return nameIndex.size();
    }
}

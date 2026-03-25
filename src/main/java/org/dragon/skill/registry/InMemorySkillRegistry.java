package org.dragon.skill.registry;

import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.model.Skill;
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

    /** 主索引：name -> SkillRuntimeEntry */
    private final ConcurrentHashMap<String, SkillRuntimeEntry> nameIndex = new ConcurrentHashMap<>();

    /** ID 索引：id -> skillName */
    private final ConcurrentHashMap<Long, String> idIndex = new ConcurrentHashMap<>();

    @Override
    public void register(SkillRuntimeEntry runtimeEntry) {
        Skill skill = runtimeEntry.getSkillEntry().getSkill();
        String name = skill.getName();
        long id = skill.getId();

        // 若已存在旧记录，先清理 ID 索引
        SkillRuntimeEntry existing = nameIndex.get(name);
        if (existing != null) {
            idIndex.remove(existing.getSkillEntry().getSkill().getId());
        }

        nameIndex.put(name, runtimeEntry);
        idIndex.put(id, name);

        log.info("Skill 已注册到运行时注册表: name={}, id={}, version={}, workspaceId={}, state={}",
                name, id, skill.getVersion(), runtimeEntry.getWorkspaceId(), runtimeEntry.getState());
    }

    @Override
    public void unregister(String skillName) {
        SkillRuntimeEntry removed = nameIndex.remove(skillName);
        if (removed != null) {
            idIndex.remove(removed.getSkillEntry().getSkill().getId());
            log.info("Skill 已从运行时注册表注销: name={}", skillName);
        }
    }

    @Override
    public Optional<SkillRuntimeEntry> findByName(String skillName) {
        return Optional.ofNullable(nameIndex.get(skillName));
    }

    @Override
    public Optional<SkillRuntimeEntry> findById(long skillId) {
        String name = idIndex.get(skillId);
        if (name == null) return Optional.empty();
        return Optional.ofNullable(nameIndex.get(name));
    }

    @Override
    public Collection<SkillRuntimeEntry> findAll() {
        return Collections.unmodifiableCollection(nameIndex.values());
    }

    @Override
    public Collection<SkillRuntimeEntry> findAllActiveByWorkspace(long workspaceId) {
        return nameIndex.values().stream()
                .filter(e -> e.getWorkspaceId() == 0L || e.getWorkspaceId() == workspaceId)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String buildSystemPromptFragment(long workspaceId) {
        Collection<SkillRuntimeEntry> entries = findAllActiveByWorkspace(workspaceId);
        if (entries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        entries.stream()
                .sorted(Comparator.comparing(e -> e.getSkillEntry().getSkill().getName()))
                .forEach(runtimeEntry -> {
                    Skill skill = runtimeEntry.getSkillEntry().getSkill();
                    String content = skill.getContent();
                    if (content != null && !content.isBlank()) {
                        sb.append("\n<skill name=\"").append(skill.getName()).append("\">\n");
                        sb.append(content.trim());
                        sb.append("\n</skill>\n");
                    }
                });

        return sb.toString().trim();
    }

    @Override
    public Map<String, SkillRuntimeState> getRuntimeStateSnapshot() {
        return nameIndex.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getState()
                ));
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

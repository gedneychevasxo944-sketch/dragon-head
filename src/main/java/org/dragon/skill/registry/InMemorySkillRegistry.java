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

    /** 主索引：name -> SkillRuntimeEntry（全局） */
    private final ConcurrentHashMap<String, SkillRuntimeEntry> nameIndex = new ConcurrentHashMap<>();

    /** ID 索引：id -> skillName */
    private final ConcurrentHashMap<Long, String> idIndex = new ConcurrentHashMap<>();

    /** Workspace 索引：workspaceId -> (skillName -> SkillRuntimeEntry) */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, SkillRuntimeEntry>> workspaceIndex = new ConcurrentHashMap<>();

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
    public void registerForWorkspace(long workspaceId, SkillRuntimeEntry runtimeEntry) {
        String skillName = runtimeEntry.getSkillEntry().getSkill().getName();
        workspaceIndex
                .computeIfAbsent(workspaceId, k -> new ConcurrentHashMap<>())
                .put(skillName, runtimeEntry);
        log.info("Skill 已注册到 workspace 注册表: workspaceId={}, skillName={}, version={}",
                workspaceId, skillName, runtimeEntry.getSkillEntry().getSkill().getVersion());
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
    public void unregisterForWorkspace(String skillName, long workspaceId) {
        ConcurrentHashMap<String, SkillRuntimeEntry> wsMap = workspaceIndex.get(workspaceId);
        if (wsMap != null) {
            wsMap.remove(skillName);
            log.info("Skill 已从 workspace 注册表注销: workspaceId={}, skillName={}", workspaceId, skillName);
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
    public Optional<SkillRuntimeEntry> findByWorkspaceAndName(long workspaceId, String skillName) {
        ConcurrentHashMap<String, SkillRuntimeEntry> wsMap = workspaceIndex.get(workspaceId);
        if (wsMap == null) return Optional.empty();
        return Optional.ofNullable(wsMap.get(skillName));
    }

    @Override
    public Collection<SkillRuntimeEntry> findAll() {
        return Collections.unmodifiableCollection(nameIndex.values());
    }

    @Override
    public Collection<SkillRuntimeEntry> findAllActiveByWorkspace(long workspaceId) {
        // 优先从 workspace 索引获取
        ConcurrentHashMap<String, SkillRuntimeEntry> wsMap = workspaceIndex.get(workspaceId);
        if (wsMap != null && !wsMap.isEmpty()) {
            return wsMap.values().stream()
                    .filter(e -> e.getState() == SkillRuntimeState.ACTIVE)
                    .collect(Collectors.toUnmodifiableList());
        }
        // 兼容旧的全局索引逻辑
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
        Map<String, SkillRuntimeState> snapshot = new LinkedHashMap<>();
        // 合并全局索引和 workspace 索引
        nameIndex.forEach((name, entry) ->
                snapshot.put(name, entry.getState()));
        workspaceIndex.forEach((wsId, wsMap) ->
                wsMap.forEach((name, entry) ->
                        snapshot.put("ws-" + wsId + "/" + name, entry.getState())));
        return Collections.unmodifiableMap(snapshot);
    }

    @Override
    public void clear() {
        nameIndex.clear();
        idIndex.clear();
        workspaceIndex.clear();
        log.info("Skill 运行时注册表已清空");
    }

    @Override
    public int size() {
        return nameIndex.size() + workspaceIndex.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}

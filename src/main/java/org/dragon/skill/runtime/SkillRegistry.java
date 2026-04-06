package org.dragon.skill.runtime;

import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.VersionType;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Skill 注册中心（设计点 1 + 3 - 多来源聚合 + Caffeine 缓存）。
 *
 * <p>职责：
 * <ol>
 *   <li><b>内置 Skill（category='builtin'）</b>：直接从 DB 查全量 active 记录，
 *       对所有 Character 无条件可见，不走绑定关系。</li>
 *   <li><b>用户 Skill</b>：通过 skill_bindings 三种关系（character / workspace /
 *       character_workspace）取并集。</li>
 *   <li><b>优先级</b>：用户绑定的 Skill 优先级高于同名的 builtin Skill。</li>
 *   <li><b>L1 缓存</b>：使用 Caffeine 按 "characterId:workspaceId" 缓存聚合结果，
 *       TTL=5分钟，保证"下一次对话"时变更即时生效。</li>
 *   <li><b>缓存失效</b>：监听 {@link SkillChangeEvent}，按受影响的 character/workspace 精确失效。</li>
 * </ol>
 *
 * <p>缓存 key 约定：
 * <pre>
 * "characterId:workspaceId"   — Character 在 Workspace 下
 * "characterId:"              — Character 独立执行（无 Workspace）
 * ":workspaceId"              — 仅 Workspace 维度（不含 Character 自有）
 * "builtin"                   — 内置 Skill 专用缓存（全局唯一，无需按 character/workspace 区分）
 * </pre>
 */
@Slf4j
@Component
public class SkillRegistry {

    private final SkillBindingStore skillBindingStore;
    private final SkillStore         skillStore;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SkillRegistry(SkillBindingStore skillBindingStore, SkillStore skillStore) {
        this.skillBindingStore = skillBindingStore;
        this.skillStore = skillStore;
    }

    /**
     * L1 缓存：key = "characterId:workspaceId"，value = SkillDefinition 列表（已聚合，未过滤）。
     * TTL 5分钟：避免每轮对话都重新查 DB，同时保证变更在下一次对话前生效。
     */
    private Cache<String, List<SkillDefinition>> cache;

    @PostConstruct
    public void init() {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
        log.info("[SkillRegistry] 初始化完成");
    }

    // ── 主查询入口 ────────────────────────────────────────────────────

    /**
     * 获取指定上下文下可用的 Skill 列表（已聚合，未过滤 isEnabled）。
     *
     * <p>返回结果为以下来源的并集（已按优先级去重）：
     * <ol>
     *   <li>DB category='builtin' 的全量 active Skill（始终可见）</li>
     *   <li>Character 自有 skill（bindingType=character）</li>
     *   <li>Workspace 公共 skill（bindingType=workspace）</li>
     *   <li>Character@Workspace 专属 skill（bindingType=character_workspace）</li>
     * </ol>
     * 用户绑定的 Skill（2/3/4）优先级高于同名的 builtin Skill（1）。
     *
     * @param characterId Character 主键（可为 null）
     * @param workspaceId Workspace 主键（可为 null）
     * @return 聚合后的 Skill 列表，同一 skillId 已去重
     */
    public List<SkillDefinition> getSkills(String characterId, String workspaceId) {
        String cacheKey = buildCacheKey(characterId, workspaceId);
        return cache.get(cacheKey, key -> loadAndMerge(characterId, workspaceId));
    }

    /**
     * 按名称查找 Skill（在已聚合列表中检索，支持 aliases 匹配）。
     */
    public SkillDefinition findByName(String characterId, String workspaceId, String name) {
        return getSkills(characterId, workspaceId).stream()
                .filter(s -> s.getName().equals(name)
                        || (s.getAliases() != null && s.getAliases().contains(name)))
                .findFirst()
                .orElse(null);
    }

    // ── 缓存失效（设计点 3 - 监听变更事件）────────────────────────────

    /**
     * 监听 SkillChangeEvent，精确失效受影响的缓存条目。
     */
    @EventListener
    public void onSkillChange(SkillChangeEvent event) {
        if (event.isGlobalEvict()) {
            log.info("[SkillRegistry] 全量缓存失效，原因: {}", event.getReason());
            cache.invalidateAll();
            return;
        }

        int evicted = 0;
        // 失效所有包含受影响 characterId 的 key
        for (String characterId : event.getAffectedCharacterIds()) {
            List<String> keys = cache.asMap().keySet().stream()
                    .filter(k -> k.startsWith(characterId + ":"))
                    .collect(Collectors.toList());
            cache.invalidateAll(keys);
            evicted += keys.size();
        }
        // 失效所有包含受影响 workspaceId 的 key
        for (String workspaceId : event.getAffectedWorkspaceIds()) {
            List<String> keys = cache.asMap().keySet().stream()
                    .filter(k -> k.endsWith(":" + workspaceId))
                    .collect(Collectors.toList());
            cache.invalidateAll(keys);
            evicted += keys.size();
        }
        log.info("[SkillRegistry] 缓存失效 {} 条，原因: {}", evicted, event.getReason());
    }

    // ── 私有加载逻辑 ─────────────────────────────────────────────────

    /**
     * 从 DB 加载并聚合 Skill 列表。
     *
     * <p>合并策略（使用 name 作为去重 key，优先级：用户绑定 > builtin）：
     * <ol>
     *   <li>先加载 builtin（优先级低，作为"底层"）</li>
     *   <li>再加载用户绑定的 Skill（优先级高，同名时覆盖 builtin）</li>
     * </ol>
     */
    private List<SkillDefinition> loadAndMerge(String characterId, String workspaceId) {
        log.debug("[SkillRegistry] cache miss，重新加载 key={}", buildCacheKey(characterId, workspaceId));

        // key = name，保证同名唯一（LinkedHashMap 维持插入顺序）
        Map<String, SkillDefinition> mergedMap = new LinkedHashMap<>();

        // 1. 加载 builtin Skill（所有 Character 全量可见，无需绑定关系）
        List<SkillDO> builtinDOs = skillStore.findAllBuiltin();
        builtinDOs.forEach(do_ -> {
            SkillDefinition def = buildDefinition(do_);
            mergedMap.put(def.getName(), def);
        });
        log.debug("[SkillRegistry] builtin Skill {} 个", builtinDOs.size());

        // 2. 加载用户绑定的 Skill（优先级高，同名时覆盖 builtin）
        List<SkillDefinition> userSkills = loadFromBindings(characterId, workspaceId);
        userSkills.forEach(def -> mergedMap.put(def.getName(), def));
        log.debug("[SkillRegistry] 用户绑定 Skill {} 个（含与 builtin 同名的覆盖）", userSkills.size());

        return new ArrayList<>(mergedMap.values());
    }

    /**
     * 从 DB 通过三种绑定关系取并集，加载当前上下文可用的用户 Skill。
     */
    private List<SkillDefinition> loadFromBindings(String characterId, String workspaceId) {

        List<SkillBindingDO> bindings;
        if (characterId != null && workspaceId != null) {
            bindings = skillBindingStore.findAvailableByCharacterAndWorkspace(characterId, workspaceId);
        } else if (characterId != null) {
            bindings = skillBindingStore.findByCharacterId(characterId);
        } else if (workspaceId != null) {
            bindings = skillBindingStore.findByWorkspaceId(workspaceId);
        } else {
            return List.of();
        }

        // 按 skillId 去重（并集中同一 skillId 可能来自多种绑定），取第一条
        return bindings.stream()
                .collect(Collectors.toMap(
                        SkillBindingDO::getSkillId,
                        b -> b,
                        (a, b) -> a))  // 同 skillId 保留第一条
                .values().stream()
                .map(this::resolveBindingToDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将绑定记录按版本策略（latest/fixed）解析为 SkillDefinition。
     */
    private SkillDefinition resolveBindingToDefinition(SkillBindingDO binding) {
        SkillDO skillDO;
        try {
            if (VersionType.FIXED.equals(binding.getVersionType())) {
                skillDO = skillStore.findBySkillIdAndVersion(
                        binding.getSkillId(), binding.getFixedVersion()).orElse(null);
            } else {
                // latest：取该 skillId 最新的 active 版本
                skillDO = skillStore.findLatestActiveBySkillId(binding.getSkillId()).orElse(null);
            }
        } catch (Exception e) {
            log.warn("[SkillRegistry] 解析 Skill {} 失败: {}", binding.getSkillId(), e.getMessage());
            return null;
        }

        if (skillDO == null || skillDO.getStatus() != SkillStatus.ACTIVE) {
            return null;
        }

        return buildDefinition(skillDO);
    }

    /**
     * 从 SkillDO 构建 SkillDefinition。
     */
    @SuppressWarnings("unchecked")
    private SkillDefinition buildDefinition(SkillDO do_) {
        List<String> allowedTools = parseJsonList(do_.getAllowedTools());
        List<String> aliases      = parseJsonList(do_.getAliases());
        List<String> tags         = parseJsonList(do_.getTags());


        final String content = do_.getContent();
        StorageInfoVO storageInfo = parseStorageInfo(do_.getStorageInfo());

        return SkillDefinition.builder()
                .category(do_.getCategory())
                .skillId(do_.getSkillId())
                .name(do_.getName())
                .version(do_.getVersion())
                .displayName(do_.getDisplayName())
                .description(do_.getDescription())
                .whenToUse(do_.getWhenToUse())
                .argumentHint(do_.getArgumentHint())
                .aliases(aliases)
                .allowedTools(allowedTools)
                .tags(tags)
                .model(do_.getModel())
                .effort(do_.getEffort())
                .executionContext(do_.getExecutionContext() != null
                        ? do_.getExecutionContext() : ExecutionContext.INLINE)
                .disableModelInvocation(Integer.valueOf(1).equals(do_.getDisableModelInvocation()))
                .userInvocable(!Integer.valueOf(0).equals(do_.getUserInvocable()))
                .persist(Integer.valueOf(1).equals(do_.getPersist()))
                .persistMode(do_.getPersistMode() != null ? do_.getPersistMode() : PersistMode.FULL)
                .storageInfo(storageInfo)
                .content(content != null ? content : "")
                .build();
    }

    // ── 工具方法 ─────────────────────────────────────────────────────

    private String buildCacheKey(String characterId, String workspaceId) {
        return (characterId != null ? characterId : "")
                + ":"
                + (workspaceId != null ? workspaceId : "");
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return OBJECT_MAPPER.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 将 storage_info JSON 字段反序列化为 StorageInfoVO。
     * 失败时返回 null，不影响正常执行（走纯文本路径）。
     */
    private StorageInfoVO parseStorageInfo(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(json, StorageInfoVO.class);
        } catch (Exception e) {
            log.warn("[SkillRegistry] storage_info 反序列化失败: {}", e.getMessage());
            return null;
        }
    }
}


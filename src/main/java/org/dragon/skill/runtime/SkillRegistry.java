package org.dragon.skill.runtime;

import org.dragon.skill.domain.SkillBindingDO;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillRuntimeConfig;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.store.SkillBindingStore;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
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
 * Skill 注册中心。
 *
 * <p>职责：
 * <ol>
 *   <li><b>内置 Skill</b>：category='builtin' 且 status=ACTIVE 的 Skill</li>
 *   <li><b>用户 Skill</b>：通过三种绑定关系获取</li>
 *   <li><b>版本解析</b>：绑定到 Skill，具体版本由 skill.publishedVersionId 决定</li>
 *   <li><b>L1 缓存</b>：Caffeine 按 characterId:workspaceId 缓存，TTL=5分钟</li>
 * </ol>
 */
@Slf4j
@Component
public class SkillRegistry {

    private final SkillBindingStore skillBindingStore;
    private final SkillStore         skillStore;
    private final SkillVersionStore  skillVersionStore;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SkillRegistry(SkillBindingStore skillBindingStore,
                         SkillStore skillStore,
                         SkillVersionStore skillVersionStore) {
        this.skillBindingStore = skillBindingStore;
        this.skillStore = skillStore;
        this.skillVersionStore = skillVersionStore;
    }

    private Cache<String, List<SkillRuntime>> cache;

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
     * 获取指定上下文下可用的 Skill 列表。
     */
    public List<SkillRuntime> getSkills(String characterId, String workspaceId) {
        String cacheKey = buildCacheKey(characterId, workspaceId);
        return cache.get(cacheKey, key -> loadAndMerge(characterId, workspaceId));
    }

    /**
     * 按名称查找 Skill。
     */
    public SkillRuntime findByName(String characterId, String workspaceId, String name) {
        return getSkills(characterId, workspaceId).stream()
                .filter(s -> s.getName().equals(name)
                        || (s.getAliases() != null && s.getAliases().contains(name)))
                .findFirst()
                .orElse(null);
    }

    // ── 缓存失效 ─────────────────────────────────────────────────────

    @EventListener
    public void onSkillChange(SkillChangeEvent event) {
        if (event.isGlobalEvict()) {
            log.info("[SkillRegistry] 全量缓存失效，原因: {}", event.getReason());
            cache.invalidateAll();
            return;
        }

        int evicted = 0;
        for (String characterId : event.getAffectedCharacterIds()) {
            List<String> keys = cache.asMap().keySet().stream()
                    .filter(k -> k.startsWith(characterId + ":"))
                    .collect(Collectors.toList());
            cache.invalidateAll(keys);
            evicted += keys.size();
        }
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

    private List<SkillRuntime> loadAndMerge(String characterId, String workspaceId) {
        log.debug("[SkillRegistry] cache miss，重新加载 key={}", buildCacheKey(characterId, workspaceId));

        Map<String, SkillRuntime> mergedMap = new LinkedHashMap<>();

        // 1. 加载 builtin Skill
        List<SkillDO> builtinDOs = skillStore.findAllBuiltin();
        for (SkillDO do_ : builtinDOs) {
            SkillRuntime def = buildDefinition(do_);
            if (def != null) {
                mergedMap.put(def.getName(), def);
            }
        }
        log.debug("[SkillRegistry] builtin Skill {} 个", builtinDOs.size());

        // 2. 加载用户绑定的 Skill
        List<SkillRuntime> userSkills = loadFromBindings(characterId, workspaceId);
        for (SkillRuntime def : userSkills) {
            mergedMap.put(def.getName(), def);
        }
        log.debug("[SkillRegistry] 用户绑定 Skill {} 个", userSkills.size());

        return new ArrayList<>(mergedMap.values());
    }

    private List<SkillRuntime> loadFromBindings(String characterId, String workspaceId) {
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

        return bindings.stream()
                .collect(Collectors.toMap(
                        SkillBindingDO::getSkillId,
                        b -> b,
                        (a, b) -> a))
                .values().stream()
                .map(this::resolveBindingToDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将绑定解析为 SkillRuntime。
     * 只返回 status=ACTIVE 且 publishedVersionId 不为 null 的 Skill。
     */
    private SkillRuntime resolveBindingToDefinition(SkillBindingDO binding) {
        SkillDO skillDO;
        try {
            skillDO = skillStore.findLatestActiveBySkillId(binding.getSkillId()).orElse(null);
        } catch (Exception e) {
            log.warn("[SkillRegistry] 解析 Skill {} 失败: {}", binding.getSkillId(), e.getMessage());
            return null;
        }

        if (skillDO == null) {
            return null;
        }

        // 获取已发布版本
        SkillVersionDO versionDO = null;
        if (skillDO.getPublishedVersionId() != null) {
            versionDO = skillVersionStore.findById(skillDO.getPublishedVersionId()).orElse(null);
        }

        if (versionDO == null || versionDO.getStatus() != SkillVersionStatus.PUBLISHED) {
            return null;
        }

        return buildDefinitionFromVersion(skillDO, versionDO);
    }

    /**
     * 从 SkillDO + SkillVersionDO 构建 SkillRuntime。
     */
    private SkillRuntime buildDefinition(SkillDO skillDO) {
        if (skillDO == null) return null;
        if (skillDO.getStatus() != SkillStatus.ACTIVE) return null;

        SkillVersionDO versionDO = null;
        if (skillDO.getPublishedVersionId() != null) {
            versionDO = skillVersionStore.findById(skillDO.getPublishedVersionId()).orElse(null);
        }

        if (versionDO == null || versionDO.getStatus() != SkillVersionStatus.PUBLISHED) {
            return null;
        }

        return buildDefinitionFromVersion(skillDO, versionDO);
    }

    private SkillRuntime buildDefinitionFromVersion(SkillDO skillDO, SkillVersionDO versionDO) {
        SkillRuntimeConfig runtimeConfig = parseRuntimeConfig(versionDO.getRuntimeConfig());
        List<String> tags = parseJsonList(skillDO.getTags());
        StorageInfoVO storageInfo = parseStorageInfo(versionDO.getStorageInfo());

        return SkillRuntime.builder()
                .category(skillDO.getCategory())
                .skillId(skillDO.getId())
                .name(versionDO.getName() != null ? versionDO.getName() : skillDO.getName())
                .version(versionDO.getVersion())
                .description(versionDO.getDescription())
                .whenToUse(runtimeConfig != null ? runtimeConfig.getWhenToUse() : null)
                .argumentHint(runtimeConfig != null ? runtimeConfig.getArgumentHint() : null)
                .aliases(runtimeConfig != null ? runtimeConfig.getAliases() : null)
                .allowedTools(runtimeConfig != null ? runtimeConfig.getAllowedTools() : null)
                .tags(tags)
                .model(runtimeConfig != null ? runtimeConfig.getModel() : null)
                .effort(runtimeConfig != null ? runtimeConfig.getEffort() : null)
                .executionContext(runtimeConfig != null && runtimeConfig.getExecutionContext() != null
                        ? runtimeConfig.getExecutionContext() : ExecutionContext.INLINE)
                .disableModelInvocation(runtimeConfig != null && Boolean.TRUE.equals(runtimeConfig.getDisableModelInvocation()))
                .userInvocable(runtimeConfig == null || !Boolean.FALSE.equals(runtimeConfig.getUserInvocable()))
                .persist(runtimeConfig != null && Boolean.TRUE.equals(runtimeConfig.getPersist()))
                .persistMode(runtimeConfig != null && runtimeConfig.getPersistMode() != null
                        ? runtimeConfig.getPersistMode() : PersistMode.FULL)
                .storageInfo(storageInfo)
                .content(versionDO.getContent() != null ? versionDO.getContent() : "")
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

    private SkillRuntimeConfig parseRuntimeConfig(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(json, SkillRuntimeConfig.class);
        } catch (Exception e) {
            log.warn("[SkillRegistry] 解析 runtimeConfig 失败: {}", json, e);
            return null;
        }
    }

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

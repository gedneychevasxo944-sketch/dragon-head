package org.dragon.skill.service;

import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillRuntimeConfig;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.dto.PageResult;
import org.dragon.skill.dto.SkillDetailVO;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillRuntimeConfigVO;
import org.dragon.skill.dto.SkillSummaryVO;
import org.dragon.skill.dto.SkillVersionSummaryVO;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SkillQueryService — Skill 查询服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillQueryService {

    private final SkillStore skillStore;
    private final SkillVersionStore skillVersionStore;
    private final SkillUsageService usageService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── 详情查询 ──────────────────────────────────────────────────────

    /**
     * 查询技能详情（包含已发布版本的内容）。
     */
    public SkillDetailVO getDetail(String skillId, boolean withContent) {
        SkillDO skill = skillStore.findBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));

        if (skill.getDeletedAt() != null) {
            throw new SkillNotFoundException(skillId);
        }

        // 获取已发布版本
        SkillVersionDO publishedVersion = null;
        if (skill.getPublishedVersionId() != null) {
            publishedVersion = skillVersionStore.findById(skill.getPublishedVersionId()).orElse(null);
        }

        return toDetailVO(skill, publishedVersion, withContent);
    }

    /**
     * 查询指定版本详情。
     */
    public SkillDetailVO getVersion(String skillId, int version) {
        SkillDO skill = skillStore.findBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));

        SkillVersionDO versionDO = skillVersionStore.findBySkillIdAndVersion(skillId, version)
                .orElseThrow(() -> new SkillNotFoundException(skillId + " v" + version));

        boolean isPublished = versionDO.getStatus() == SkillVersionStatus.PUBLISHED;
        return toDetailVO(skill, versionDO, true);
    }

    /**
     * 查询所有版本列表。
     */
    public List<SkillVersionSummaryVO> listVersions(String skillId) {
        if (!skillStore.existsBySkillId(skillId)) {
            throw new SkillNotFoundException(skillId);
        }
        List<SkillVersionDO> versions = skillVersionStore.findAllBySkillId(skillId);

        return versions.stream()
                .map(this::toVersionSummaryVO)
                .collect(Collectors.toList());
    }

    // ── 分页检索 ──────────────────────────────────────────────────────

    /**
     * 多条件分页检索（管理后台列表页）。
     */
    public PageResult<SkillSummaryVO> pageSearch(SkillQueryRequest req) {
        String categoryValue  = req.getCategory()  != null ? req.getCategory().getValue()  : null;
        String visibilityValue = req.getVisibility() != null ? req.getVisibility().getValue() : null;
        String statusValue = req.getStatus() != null ? req.getStatus().getValue() : null;

        int offset = req.getOffset();
        int limit  = req.getClampedPageSize();

        List<SkillDO> rows = skillStore.search(
                req.getKeyword(), statusValue, categoryValue, visibilityValue, req.getCreatorId(),
                req.getSortBy(), req.getSortOrder(), offset, limit);

        int total = skillStore.countSearch(
                req.getKeyword(), statusValue, categoryValue, visibilityValue, req.getCreatorId());

        List<SkillSummaryVO> items = rows.stream()
                .map(this::toSummaryVO)
                .collect(Collectors.toList());

        return PageResult.of(items, total, req.getPage(), limit);
    }

    // ── 转换方法 ──────────────────────────────────────────────────────

    private SkillDetailVO toDetailVO(SkillDO skill, SkillVersionDO version, boolean withContent) {
        SkillRuntimeConfig runtimeConfig = version != null ? parseRuntimeConfig(version.getRuntimeConfig()) : null;
        SkillRuntimeConfigVO runtimeConfigVO = toRuntimeConfigVO(runtimeConfig);

        return SkillDetailVO.builder()
                .id(skill != null ? skill.getId() : (version != null ? version.getSkillId() : null))
                .name(version != null ? version.getName() : (skill != null ? skill.getName() : null))
                .description(version != null ? version.getDescription() : (skill != null ? skill.getDescription() : null))
                .tags(skill != null ? parseJsonList(skill.getTags()) : null)
                .category(skill != null ? skill.getCategory() : null)
                .visibility(skill != null ? skill.getVisibility() : null)
                .status(skill != null ? skill.getStatus() : null)
                .version(version != null ? version.getVersion() : null)
                .versionStatus(version != null ? version.getStatus() : null)
                .content(withContent && version != null ? version.getContent() : null)
                .frontmatter(version != null ? version.getFrontmatter() : null)
                .runtimeConfig(runtimeConfigVO)
                .storageType(version != null ? version.getStorageType() : null)
                .storageInfo(version != null ? parseStorageInfo(version.getStorageInfo()) : null)
                .creatorId(skill != null ? skill.getCreatorId() : null)
                .creatorName(skill != null ? skill.getCreatorName() : null)
                .createdAt(skill != null ? skill.getCreatedAt() : null)
                .editorId(version != null ? version.getEditorId() : null)
                .editorName(version != null ? version.getEditorName() : null)
                .publishedAt(version != null ? version.getPublishedAt() : null)
                .build();
    }

    private SkillRuntimeConfigVO toRuntimeConfigVO(SkillRuntimeConfig config) {
        if (config == null) return null;
        return SkillRuntimeConfigVO.builder()
                .aliases(config.getAliases())
                .whenToUse(config.getWhenToUse())
                .argumentHint(config.getArgumentHint())
                .allowedTools(config.getAllowedTools())
                .model(config.getModel())
                .effort(config.getEffort())
                .executionContext(config.getExecutionContext())
                .disableModelInvocation(config.getDisableModelInvocation())
                .userInvocable(config.getUserInvocable())
                .persist(config.getPersist())
                .persistMode(config.getPersistMode())
                .build();
    }

    private SkillSummaryVO toSummaryVO(SkillDO s) {
        // 获取已发布版本号
        Integer publishedVersion = null;
        if (s.getPublishedVersionId() != null) {
            publishedVersion = skillVersionStore.findById(s.getPublishedVersionId())
                    .map(SkillVersionDO::getVersion).orElse(null);
        }

        return SkillSummaryVO.builder()
                .id(s.getId())
                .publishedVersion(publishedVersion)
                .name(s.getName())
                .description(s.getDescription())
                .category(s.getCategory())
                .visibility(s.getVisibility())
                .status(s.getStatus())
                .creatorId(s.getCreatorId())
                .creatorName(s.getCreatorName())
                .createdAt(s.getCreatedAt())
                .tags(parseJsonList(s.getTags()))
                .build();
    }

    private SkillVersionSummaryVO toVersionSummaryVO(SkillVersionDO v) {
        return SkillVersionSummaryVO.builder()
                .version(v.getVersion())
                .versionStatus(v.getStatus())
                .editorId(v.getEditorId())
                .editorName(v.getEditorName())
                .createdAt(v.getCreatedAt())
                .releaseNote(v.getReleaseNote())
                .build();
    }

    // ── 私有工具 ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[SkillQuery] JSON 解析失败: {}", json);
            return null;
        }
    }

    private SkillRuntimeConfig parseRuntimeConfig(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, SkillRuntimeConfig.class);
        } catch (Exception e) {
            log.warn("[SkillQuery] 解析 runtimeConfig 失败: {}", json);
            return null;
        }
    }

    private StorageInfoVO parseStorageInfo(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readValue(json, StorageInfoVO.class);
        } catch (Exception e) {
            log.warn("[SkillQuery] 解析 storageInfo 失败: {}", json);
            return null;
        }
    }
}

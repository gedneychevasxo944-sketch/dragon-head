package org.dragon.skill.service;

import org.dragon.expert.service.ExpertService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.dto.PageResult;
import org.dragon.skill.dto.SkillDetailVO;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillSummaryVO;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.store.SkillStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SkillQueryService — Skill 查询服务（详情 / 版本列表 / 分页检索）。
 *
 * <h3>接口概览</h3>
 * <ul>
 *   <li>{@link #getDetail(String, boolean)} — 查询最新版本详情（含/不含 content 正文）</li>
 *   <li>{@link #getVersion(String, int)} — 查询指定版本详情</li>
 *   <li>{@link #listVersions(String)} — 查询某 skillId 的全部版本列表</li>
 *   <li>{@link #pageSearch(SkillQueryRequest)} — 多条件分页检索</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillQueryService {

    private final SkillStore skillStore;
    private final SkillUsageService usageService;
    private final ExpertService expertService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── 详情查询 ──────────────────────────────────────────────────────

    /**
     * 查询指定 skillId 的最新版本详情。
     *
     * @param skillId     技能业务 UUID
     * @param withContent 是否包含 SKILL.md 正文（详情页 true，摘要场景 false）
     * @return 技能详情 VO
     * @throws SkillNotFoundException skillId 不存在时抛出
     */
    public SkillDetailVO getDetail(String skillId, boolean withContent) {
        SkillDO skill = skillStore.findLatestBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));
        int maxVersion = skill.getVersion();
        return toDetailVO(skill, true, withContent, maxVersion);
    }

    /**
     * 查询指定 skillId + version 的版本详情（含 content 正文）。
     *
     * @param skillId 技能业务 UUID
     * @param version 版本号
     * @return 技能详情 VO
     * @throws SkillNotFoundException 未找到对应记录时抛出
     */
    public SkillDetailVO getVersion(String skillId, int version) {
        SkillDO skill = skillStore.findBySkillIdAndVersion(skillId, version)
                .orElseThrow(() -> new SkillNotFoundException(skillId + " v" + version));
        int maxVersion = skillStore.findMaxVersionBySkillId(skillId);
        return toDetailVO(skill, skill.getVersion() == maxVersion, true, maxVersion);
    }

    /**
     * 查询指定 skillId 的所有版本列表（摘要，不含 content 正文）。
     *
     * @param skillId 技能业务 UUID
     * @return 按版本升序排列的摘要列表
     * @throws SkillNotFoundException skillId 不存在时抛出
     */
    public List<SkillDetailVO> listVersions(String skillId) {
        if (!skillStore.existsBySkillId(skillId)) {
            throw new SkillNotFoundException(skillId);
        }
        List<SkillDO> versions = skillStore.findAllVersionsBySkillId(skillId);
        if (versions.isEmpty()) {
            return Collections.emptyList();
        }
        int maxVersion = versions.stream()
                .mapToInt(SkillDO::getVersion)
                .max()
                .orElse(0);
        return versions.stream()
                .map(s -> toDetailVO(s, s.getVersion() == maxVersion, false, maxVersion))
                .collect(Collectors.toList());
    }

    // ── 分页检索 ──────────────────────────────────────────────────────

    /**
     * 多条件分页检索（管理后台列表页）。
     *
     * <p>每个 skillId 只返回最新版本；deleted 状态记录始终排除。
     *
     * @param req 检索参数（keyword / status / category / visibility / creatorId / 分页排序）
     * @return 分页结果，列表项为 {@link SkillSummaryVO}
     */
    public PageResult<SkillSummaryVO> pageSearch(SkillQueryRequest req) {
        // deleted 不对外暴露，即使请求方传入也强制清除
        String statusValue = null;
        if (req.getStatus() != null
                && !"deleted".equalsIgnoreCase(req.getStatus().getValue())) {
            statusValue = req.getStatus().getValue();
        }

        String categoryValue  = req.getCategory()  != null ? req.getCategory().getValue()  : null;
        String visibilityValue = req.getVisibility() != null ? req.getVisibility().getValue() : null;

        int offset = req.getOffset();
        int limit  = req.getClampedPageSize();

        // 并行执行数据查询和总数统计
        List<SkillDO> rows = skillStore.search(
                req.getKeyword(), statusValue, categoryValue, visibilityValue, req.getCreatorId(),
                req.getSortBy(), req.getSortOrder(), offset, limit);

        int total = skillStore.countSearch(
                req.getKeyword(), statusValue, categoryValue, visibilityValue, req.getCreatorId());

        List<SkillSummaryVO> items = rows.stream()
                .map(this::toSummaryVO)
                .collect(Collectors.toList());

        // 过滤掉 Expert 标记的资产
        Set<String> nonExpertIds = expertService.filterOutExpertMarked(
                ResourceType.SKILL,
                items.stream().map(SkillSummaryVO::getSkillId).collect(Collectors.toList()));
        items = items.stream()
                .filter(s -> nonExpertIds.contains(s.getSkillId()))
                .collect(Collectors.toList());

        return PageResult.of(items, total, req.getPage(), limit);
    }

    // ── 转换方法 ──────────────────────────────────────────────────────

    private SkillDetailVO toDetailVO(SkillDO s, boolean isLatest,
                                     boolean withContent, int maxVersion) {
        return SkillDetailVO.builder()
                .id(s.getId())
                .skillId(s.getSkillId())
                .version(s.getVersion())
                .isLatest(isLatest)
                .name(s.getName())
                .displayName(s.getDisplayName())
                .description(s.getDescription())
                .whenToUse(s.getWhenToUse())
                .argumentHint(s.getArgumentHint())
                .content(withContent ? s.getContent() : null)
                .aliases(parseJsonList(s.getAliases()))
                .allowedTools(parseJsonList(s.getAllowedTools()))
                .model(s.getModel())
                .disableModelInvocation(toBool(s.getDisableModelInvocation()))
                .userInvocable(toBool(s.getUserInvocable()))
                .executionContext(s.getExecutionContext())
                .effort(s.getEffort())
                .persist(toBool(s.getPersist()))
                .persistMode(s.getPersistMode())
                .category(s.getCategory())
                .visibility(s.getVisibility())
                .status(s.getStatus())
                .storageType(s.getStorageType())
                .creatorId(s.getCreatorId())
                .creatorName(s.getCreatorName())
                .editorId(s.getEditorId())
                .editorName(s.getEditorName())
                .createdAt(s.getCreatedAt())
                .publishedAt(s.getPublishedAt())
                .build();
    }

    private SkillSummaryVO toSummaryVO(SkillDO s) {
        return SkillSummaryVO.builder()
                .skillId(s.getSkillId())
                .version(s.getVersion())
                .name(s.getName())
                .displayName(s.getDisplayName())
                .description(s.getDescription())
                .whenToUse(s.getWhenToUse())
                .argumentHint(s.getArgumentHint())
                .category(s.getCategory())
                .visibility(s.getVisibility())
                .status(s.getStatus())
                .executionContext(s.getExecutionContext())
                .creatorId(s.getCreatorId())
                .creatorName(s.getCreatorName())
                .createdAt(s.getCreatedAt())
                .publishedAt(s.getPublishedAt())
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

    private Boolean toBool(Integer val) {
        if (val == null) return null;
        return val == 1;
    }
}


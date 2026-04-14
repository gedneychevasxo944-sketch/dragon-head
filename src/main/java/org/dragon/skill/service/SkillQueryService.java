package org.dragon.skill.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.character.service.CharacterService;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.skill.dto.SkillVO;
import org.dragon.skill.dto.SkillVO.CharacterRef;
import org.dragon.skill.dto.SkillVO.WorkspaceRef;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillVersionVO;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
    private final AssetAssociationService assetAssociationService;
    private final CharacterService characterService;
    private final WorkspaceRegistry workspaceRegistry;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── 详情查询 ──────────────────────────────────────────────────────

    /**
     * 查询技能详情（包含已发布版本的内容）。
     */
    public SkillVO getDetail(String skillId) {
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

        return toDetailVO(skill, publishedVersion);
    }

    /**
     * 查询所有版本列表。
     */
    public List<SkillVersionVO> listVersions(String skillId) {
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
    public PageResponse<SkillVO> pageSearch(SkillQueryRequest req) {
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

        List<SkillVO> items = rows.stream()
                .map(this::toItemVO)
                .collect(Collectors.toList());

        return PageResponse.of(items, total, req.getPage(), limit);
    }

    // ── 转换方法 ──────────────────────────────────────────────────────

    private SkillVO toDetailVO(SkillDO skill, SkillVersionDO version) {
        String skillId = skill != null ? skill.getId() : (version != null ? version.getSkillId() : null);

        // 查询关联的 Character 列表
        List<CharacterRef> characterRefs = Collections.emptyList();
        if (skillId != null) {
            characterRefs = assetAssociationService.getCharactersForSkill(skillId).stream()
                    .map(characterId -> characterService.getCharacter(characterId)
                            .map(c -> CharacterRef.builder()
                                    .id(c.getId())
                                    .name(c.getName())
                                    .avatar(c.getAvatar())
                                    .build())
                            .orElse(null))
                    .filter(ref -> ref != null)
                    .collect(Collectors.toList());
        }

        // 查询关联的 Workspace 列表
        List<WorkspaceRef> workspaceRefs = Collections.emptyList();
        if (skillId != null) {
            workspaceRefs = assetAssociationService.getWorkspacesForSkill(skillId).stream()
                    .map(workspaceId -> workspaceRegistry.get(workspaceId)
                            .map(w -> WorkspaceRef.builder()
                                    .id(w.getId())
                                    .name(w.getName())
                                    .build())
                            .orElse(null))
                    .filter(ref -> ref != null)
                    .collect(Collectors.toList());
        }

        return SkillVO.builder()
                .id(skillId)
                .name(version != null ? version.getName() : (skill != null ? skill.getName() : null))
                .displayName(skill != null ? skill.getDisplayName() : null)
                .introduction(skill != null ? skill.getIntroduction() : null)
                .description(version != null ? version.getDescription() : null)
                .tags(skill != null ? parseJsonList(skill.getTags()) : null)
                .category(skill != null ? skill.getCategory() : null)
                .visibility(skill != null ? skill.getVisibility() : null)
                .status(skill != null ? skill.getStatus() : null)
                .version(version != null ? version.getVersion() : null)
                .versionStatus(version != null ? version.getStatus() : null)
                .creatorId(skill != null ? skill.getCreatorId() : null)
                .creatorName(skill != null ? skill.getCreatorName() : null)
                .createdAt(skill != null ? skill.getCreatedAt() : null)
                .editorId(version != null ? version.getEditorId() : null)
                .editorName(version != null ? version.getEditorName() : null)
                .publishedAt(version != null ? version.getPublishedAt() : null)
                .characters(characterRefs)
                .workspaces(workspaceRefs)
                .build();
    }

    private SkillVO toItemVO(SkillDO s) {
        // 获取已发布版本号
        if (s.getPublishedVersionId() != null) {
            skillVersionStore.findById(s.getPublishedVersionId())
                    .map(SkillVersionDO::getVersion);
        }

        return SkillVO.builder()
                .id(s.getId())
                .displayName(s.getDisplayName())
                .category(s.getCategory())
                .visibility(s.getVisibility())
                .status(s.getStatus())
                .creatorId(s.getCreatorId())
                .creatorName(s.getCreatorName())
                .createdAt(s.getCreatedAt())
                .tags(parseJsonList(s.getTags()))
                .build();
    }

    private SkillVersionVO toVersionSummaryVO(SkillVersionDO v) {
        return SkillVersionVO.builder()
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
}

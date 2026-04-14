package org.dragon.skill.service;

import org.dragon.actionlog.ActionType;
import org.dragon.permission.enums.ResourceType;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.service.AssetMemberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.skill.domain.ParsedSkillContent;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.SkillRuntimeConfig;
import org.dragon.skill.domain.SkillVersionDO;
import org.dragon.skill.dto.StorageInfo;
import org.dragon.skill.dto.SkillRegisterRequest;
import org.dragon.skill.dto.SkillRegisterResult;
import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVersionStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.store.SkillVersionStore;
import org.dragon.skill.util.SkillContentParser;
import org.dragon.skill.util.SkillValidator;
import org.dragon.util.bean.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Skill 注册与更新服务。
 *
 * <p>核心设计：
 * <ul>
 *   <li>SkillDO：技能元信息（不含版本内容）</li>
 *   <li>SkillVersionDO：技能版本内容（每次更新生成新版本）</li>
 *   <li>注册：创建 SkillDO + V1 draft 版本</li>
 *   <li>更新：创建新 draft 版本（不改变已发布版本）</li>
 * </ul>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SkillRegisterService {

    @Autowired private SkillFileService storageService;
    @Autowired private SkillStore          skillStore;
    @Autowired private SkillVersionStore   skillVersionStore;
    @Autowired private SkillActionLogService actionLogService;
    @Autowired private AssetMemberService assetMemberService;
    @Autowired private AssetPublishStatusService publishStatusService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ── 注册接口 ─────────────────────────────────────────────────────

    /**
     * 注册新技能（ZIP 方式）。
     */
    public SkillRegisterResult register(MultipartFile zipFile,
                                        SkillRegisterRequest request,
                                        UserInfo user) {
        String skillId = UUID.randomUUID().toString();
        Long operatorId = parseUserId(user.getUserId());
        String operatorName = user.getUsername();

        // 创建技能元信息 + V1 版本
        SkillDO skill = doCreateSkill(skillId, 1, operatorId, operatorName, zipFile, request, operatorId, operatorName);

        assetMemberService.addOwnerDirectly(ResourceType.SKILL, skillId, operatorId);
                // 初始化发布状态（默认为 DRAFT）

        publishStatusService.initializeStatus(ResourceType.SKILL, skillId, String.valueOf(operatorId));
        // 记录操作日志
        actionLogService.log(skillId, skill.getName(), skill.getDisplayName(),
                ActionType.SKILL_REGISTER, operatorName, 1);

        return SkillRegisterResult.builder()
                .skillId(skillId)
                .version(1)
                .status(SkillStatus.DRAFT.getValue())
                .build();
    }

    // ── 更新接口 ─────────────────────────────────────────────────────

    /**
     * 更新已有技能（ZIP 方式）。
     */
    public SkillRegisterResult update(String skillId,
                                       MultipartFile zipFile,
                                       SkillRegisterRequest request,
                                       UserInfo user) {
        // 1. 校验 skillId 存在且未删除
        SkillDO existing = requireNotDeleted(skillId);

        // 2. 计算新版本号
        int newVersion = skillVersionStore.findMaxVersionBySkillId(skillId) + 1;

        Long operatorId = parseUserId(user.getUserId());
        String operatorName = user.getUsername();

        // 创建新版本（draft）
        SkillVersionDO version = doCreateVersion(skillId, newVersion, existing.getCreatorId(),
                existing.getCreatorName(), operatorId, operatorName, zipFile, request);

        actionLogService.log(skillId,
                request.getName() != null ? request.getName() : existing.getName(),
                request.getDisplayName() != null ? request.getDisplayName() : existing.getDisplayName(),
                ActionType.SKILL_UPDATE, operatorName, newVersion);

        return SkillRegisterResult.builder()
                .skillId(skillId)
                .version(newVersion)
                .status(SkillStatus.DRAFT.getValue())
                .build();
    }

    /**
     * 保存草稿。
     */
    public SkillRegisterResult saveDraft(String skillId, SkillRegisterRequest request, UserInfo user) {
        SkillDO existing = requireNotDeleted(skillId);
        Long operatorId = parseUserId(user.getUserId());
        String operatorName = user.getUsername();

        // 查找现有 draft 版本
        SkillVersionDO draftVersion = skillVersionStore.findDraftBySkillId(skillId)
                .orElse(null);

        int versionToReturn;
        if (draftVersion != null) {
            // 更新现有草稿
            updateVersionFromRequest(draftVersion, request, operatorId, operatorName);
            skillVersionStore.update(draftVersion);
            versionToReturn = draftVersion.getVersion();
        } else {
            // 创建新草稿版本
            int newVersion = skillVersionStore.findMaxVersionBySkillId(skillId) + 1;
            doCreateVersion(skillId, newVersion, existing.getCreatorId(),
                    existing.getCreatorName(), operatorId, operatorName, null, request);
            versionToReturn = newVersion;
        }

        actionLogService.log(skillId, existing.getName(), existing.getDisplayName(),
                ActionType.SKILL_SAVE_DRAFT, operatorName, versionToReturn);

        return SkillRegisterResult.builder()
                .skillId(skillId)
                .version(versionToReturn)
                .status(SkillStatus.DRAFT.getValue())
                .build();
    }

    // ── 统一内部方法 ─────────────────────────────────────────────────

    /**
     * 创建新技能：SkillDO + V1 版本。
     */
    private SkillDO doCreateSkill(String skillId, int version,
                                   Long creatorId, String creatorName,
                                   MultipartFile zipFile, SkillRegisterRequest request,
                                   Long operatorId, String operatorName) {
        // 1. 解析 ZIP
        ParsedSkillContent parsed = parseAndMerge(zipFile, request);

        // 2. 上传文件
        StorageInfo storageInfo = storageService.upload(skillId, version, parsed.getFileMap());

        // 3. 构建并保存 SkillDO
        SkillDO skill = new SkillDO();
        skill.setId(skillId);
        skill.setName(parsed.getName());
        skill.setIntroduction(parsed.getDescription());
        skill.setCategory(SkillCategory.fromValue(parsed.getCategory()));
        skill.setVisibility(SkillVisibility.fromValue(parsed.getVisibility()));
        skill.setTags(toJson(request != null ? request.getTags() : null));
        skill.setCreatorType(CreatorType.PERSONAL);
        skill.setCreatorId(creatorId != null ? creatorId : operatorId);
        skill.setCreatorName(creatorName != null ? creatorName : operatorName);
        skill.setStatus(SkillStatus.DRAFT);
        skillStore.save(skill);

        // 4. 构建并保存 V1 版本
        SkillVersionDO versionDO = buildSkillVersionDO(skillId, version, parsed, storageInfo,
                operatorId, operatorName, request);
        skillVersionStore.save(versionDO);

        log.info("[SkillRegisterService] Created skill: skillId={}, version={}", skillId, version);
        return skill;
    }

    /**
     * 创建新版本。
     */
    private SkillVersionDO doCreateVersion(String skillId, int version,
                                          Long creatorId, String creatorName,
                                          Long editorId, String editorName,
                                          MultipartFile zipFile, SkillRegisterRequest request) {
        // 1. 解析 ZIP
        ParsedSkillContent parsed = parseAndMerge(zipFile, request);

        // 2. 上传文件
        StorageInfo storageInfo = storageService.upload(skillId, version, parsed.getFileMap());

        // 3. 构建并保存版本
        SkillVersionDO versionDO = buildSkillVersionDO(skillId, version, parsed, storageInfo,
                editorId, editorName, request);
        skillVersionStore.save(versionDO);

        log.info("[SkillRegisterService] Created version: skillId={}, version={}", skillId, version);
        return versionDO;
    }

    /**
     * 解析并合并 ZIP 和 request 元数据。
     */
    private ParsedSkillContent parseAndMerge(MultipartFile zipFile, SkillRegisterRequest request) {
        SkillValidator.validateZipFile(zipFile);
        ParsedSkillContent parsed = SkillContentParser.parseFromZip(zipFile);
        mergeRequestToParsed(parsed, request);
        SkillValidator.validateSkillContent(parsed);
        return parsed;
    }

    // ── 版本构建 ─────────────────────────────────────────────────────

    /**
     * 构建 SkillVersionDO。
     */
    private SkillVersionDO buildSkillVersionDO(String skillId, int version,
                                               ParsedSkillContent parsed, StorageInfo storageInfo,
                                               Long editorId, String editorName,
                                               SkillRegisterRequest request) {
        SkillVersionDO v = new SkillVersionDO();
        v.setSkillId(skillId);
        v.setVersion(version);
        v.setName(parsed.getName());
        v.setDescription(parsed.getDescription());
        v.setContent(parsed.getBodyContent());
        v.setFrontmatter(parsed.getFrontmatter());

        // 构建运行时配置
        SkillRuntimeConfig runtimeConfig = buildRuntimeConfigFromParsed(parsed);
        v.setRuntimeConfig(toJson(runtimeConfig));

        v.setEditorId(editorId);
        v.setEditorName(editorName);
        v.setStatus(SkillVersionStatus.DRAFT);
        v.setStorageInfo(toJson(storageInfo));
        return v;
    }

    /**
     * 从 ParsedSkillContent 构建运行时配置对象。
     */
    private SkillRuntimeConfig buildRuntimeConfigFromParsed(ParsedSkillContent parsed) {
        SkillRuntimeConfig config = new SkillRuntimeConfig();
        config.setWhenToUse(parsed.getWhenToUse());
        config.setArgumentHint(parsed.getArgumentHint());
        config.setAliases(parsed.getAliases());
        config.setAllowedTools(parsed.getAllowedTools());
        config.setModel(parsed.getModel());
        config.setDisableModelInvocation(parsed.getDisableModelInvocation());
        config.setUserInvocable(parsed.getUserInvocable());
        config.setExecutionContext(ExecutionContext.fromValue(
                parsed.getExecutionContext() != null ? parsed.getExecutionContext() : "inline"));
        config.setEffort(SkillEffort.fromValue(parsed.getEffort()));
        config.setPersist(false);
        config.setPersistMode(PersistMode.FULL);
        return config;
    }

    /**
     * 用 request 更新版本内容。
     */
    private void updateVersionFromRequest(SkillVersionDO version, SkillRegisterRequest request,
                                         Long editorId, String editorName) {
        if (request == null) return;

        if (StringUtils.hasText(request.getContent())) {
            version.setContent(request.getContent());
        }
        if (request.getAliases() != null) {
            SkillRuntimeConfig runtimeConfig = parseRuntimeConfig(version.getRuntimeConfig());
            if (runtimeConfig == null) runtimeConfig = new SkillRuntimeConfig();
            runtimeConfig.setAliases(request.getAliases());
            version.setRuntimeConfig(toJson(runtimeConfig));
        }
        version.setEditorId(editorId);
        version.setEditorName(editorName);
    }

    /**
     * 用 request 元数据补充/覆盖 ParsedContent。
     */
    private void mergeRequestToParsed(ParsedSkillContent parsed, SkillRegisterRequest request) {
        if (request == null) return;

        if (StringUtils.hasText(request.getName())) {
            parsed.setName(request.getName());
        }
        if (request.getDisplayName() != null) {
            parsed.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            parsed.setDescription(request.getDescription()); // ParsedSkillContent 仍使用 description
        }
        if (StringUtils.hasText(request.getContent())) {
            parsed.setBodyContent(request.getContent());
        }
        if (StringUtils.hasText(request.getVisibility())) {
            parsed.setVisibility(request.getVisibility());
        }
        if (StringUtils.hasText(request.getCategory())) {
            parsed.setCategory(request.getCategory());
        }
        if (request.getAliases() != null) {
            parsed.setAliases(request.getAliases());
        }
        if (StringUtils.hasText(request.getWhenToUse())) {
            parsed.setWhenToUse(request.getWhenToUse());
        }
        if (StringUtils.hasText(request.getArgumentHint())) {
            parsed.setArgumentHint(request.getArgumentHint());
        }
        if (request.getAllowedTools() != null) {
            parsed.setAllowedTools(request.getAllowedTools());
        }
        if (StringUtils.hasText(request.getModel())) {
            parsed.setModel(request.getModel());
        }
        if (StringUtils.hasText(request.getExecutionContext())) {
            parsed.setExecutionContext(request.getExecutionContext());
        }
        if (StringUtils.hasText(request.getEffort())) {
            parsed.setEffort(request.getEffort());
        }
        if (request.getDisableModelInvocation() != null) {
            parsed.setDisableModelInvocation(request.getDisableModelInvocation());
        }
        if (request.getUserInvocable() != null) {
            parsed.setUserInvocable(request.getUserInvocable());
        }
    }

    // ── 私有辅助 ─────────────────────────────────────────────────────

    /** 查询技能，若不存在或已删除则抛出异常 */
    private SkillDO requireNotDeleted(String skillId) {
        SkillDO skill = skillStore.findBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));
        if (skill.getDeletedAt() != null) {
            throw new SkillNotFoundException(skillId);
        }
        return skill;
    }

    private SkillRuntimeConfig parseRuntimeConfig(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(json, SkillRuntimeConfig.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillRegisterService.class);
}

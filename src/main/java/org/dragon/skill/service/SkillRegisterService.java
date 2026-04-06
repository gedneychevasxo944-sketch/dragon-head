package org.dragon.skill.service;

import org.dragon.skill.actionlog.SkillActionLogService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.permission.service.CollaboratorService;
import org.dragon.skill.domain.ParsedSkillContent;
import org.dragon.skill.domain.SkillDO;
import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.dto.SkillRegisterRequest;
import org.dragon.skill.dto.SkillRegisterResult;
import org.dragon.skill.enums.CreatorType;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.enums.StorageType;
import org.dragon.skill.exception.SkillNotFoundException;
import org.dragon.skill.store.SkillStore;
import org.dragon.skill.util.SkillContentParser;
import org.dragon.skill.util.SkillValidator;
import org.dragon.util.bean.UserInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Skill 注册与更新服务。
 *
 * <p>核心设计：ZIP 方式统一处理注册和更新
 * <pre>
 *   1. 前端上传 ZIP 文件，解析 SKILL.md 获取元数据
 *   2. 前端将 ZIP + 解析的元数据一起发送到后端
 *   3. 后端合并元数据：ZIP 中的元数据优先，request body 作为补充
 *   4. 注册和更新分开：register() 用于首次注册，update() 用于更新已有技能
 * </pre>
 *
 * <p>版本策略：
 * <ul>
 *   <li>首次注册：skillId = 新生成的 UUID，version = 1</li>
 *   <li>更新：skillId 不变，version = MAX(version) + 1，INSERT 新记录</li>
 *   <li>新版本状态始终为 draft，旧版本记录保持原状态不变</li>
 * </ul>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SkillRegisterService {

    @Autowired private SkillStorageService storageService;
    @Autowired private SkillStore          skillStore;
    @Autowired private SkillActionLogService actionLogService;
    @Autowired private CollaboratorService collaboratorService;
    @Autowired private AssetPublishStatusService publishStatusService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ── 注册接口 ─────────────────────────────────────────────────────

    /**
     * 注册新技能（ZIP 方式）。
     *
     * <p>元数据合并策略：ZIP 中的 frontmatter 元数据优先，request body 作为补充覆盖。
     *
     * @param zipFile  ZIP 文件（必填，包含 SKILL.md 和可选附件）
     * @param request  前端已解析的元数据（会与 ZIP 元数据合并）
     * @param user     操作者用户信息
     * @return 注册结果
     */
    public SkillRegisterResult register(MultipartFile zipFile,
                                        SkillRegisterRequest request,
                                        UserInfo user) {
        String skillId = UUID.randomUUID().toString();
        Long operatorId = parseUserId(user.getUserId());
        String operatorName = user.getUsername();
        SkillRegisterResult result = doSave(skillId, 1, null, null, zipFile, request, operatorId, operatorName);
        collaboratorService.addOwnerDirectly(ResourceType.SKILL, skillId, operatorId);
                // 初始化发布状态（默认为 DRAFT）
                
        publishStatusService.initializeStatus(ResourceType.SKILL, skillId, String.valueOf(operatorId));
        // 记录操作日志
        actionLogService.log(skillId, request.getName(),
                org.dragon.skill.enums.SkillActionType.REGISTER,
                operatorId, operatorName, 1);
        return result;
    }

    // ── 更新接口 ─────────────────────────────────────────────────────

    /**
     * 更新已有技能（ZIP 方式）。
     *
     * <p>元数据合并策略：ZIP 中的 frontmatter 元数据优先，request body 作为补充覆盖。
     *
     * @param skillId  技能 UUID
     * @param zipFile  ZIP 文件（必填，包含 SKILL.md 和可选附件）
     * @param request  前端已解析的元数据（会与 ZIP 元数据合并）
     * @param user     操作者用户信息
     * @return 更新结果
     */
    public SkillRegisterResult update(String skillId,
                                       MultipartFile zipFile,
                                       SkillRegisterRequest request,
                                       UserInfo user) {
        // 1. 校验 skillId 存在且未删除
        SkillDO existing = requireLatestNotDeleted(skillId);

        // 2. 计算新版本号
        int newVersion = skillStore.findMaxVersionBySkillId(skillId) + 1;

        Long operatorId = parseUserId(user.getUserId());
        String operatorName = user.getUsername();
        SkillRegisterResult result = doSave(skillId, newVersion, existing.getCreatorId(), existing.getCreatorName(),
                zipFile, request, operatorId, operatorName);

        // 记录操作日志
        actionLogService.log(skillId, request.getName() != null ? request.getName() : existing.getName(),
                org.dragon.skill.enums.SkillActionType.UPDATE,
                operatorId, operatorName, newVersion);

        return result;
    }

    /**
     * 保存草稿。
     *
     * <p>仅更新最新版本的元数据（content 等），不创建新版本，不上传文件。
     * 状态保持 DRAFT 不变。
     *
     * @param skillId 技能 UUID
     * @param request 草稿内容
     * @return 注册结果
     */
    public SkillRegisterResult saveDraft(String skillId, SkillRegisterRequest request) {
        // 1. 校验 skillId 存在且未删除
        SkillDO existing = requireLatestNotDeleted(skillId);

        // 2. 更新草稿字段
        applyRequestToSkillDO(existing, request);
        existing.setStatus(SkillStatus.DRAFT);
        skillStore.update(existing);

        // 记录操作日志
        actionLogService.log(skillId, existing.getName(),
                org.dragon.skill.enums.SkillActionType.SAVE_DRAFT,
                null, null, existing.getVersion());

        // 3. 返回注册结果
        return SkillRegisterResult.builder()
                .skillId(skillId)
                .version(existing.getVersion())
                .status(SkillStatus.DRAFT.getValue())
                .build();
    }

    /**
     * 将 request 的字段应用到 SkillDO（不覆盖 null 字段）。
     */
    private void applyRequestToSkillDO(SkillDO skill, SkillRegisterRequest request) {
        if (request == null) {
            return;
        }
        if (StringUtils.hasText(request.getName())) {
            skill.setName(request.getName());
        }
        if (request.getDisplayName() != null) {
            skill.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            skill.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getContent())) {
            skill.setContent(request.getContent());
        }
        if (StringUtils.hasText(request.getVisibility())) {
            skill.setVisibility(SkillVisibility.fromValue(request.getVisibility()));
        }
        if (StringUtils.hasText(request.getCategory())) {
            skill.setCategory(SkillCategory.fromValue(request.getCategory()));
        }
        if (request.getAliases() != null) {
            skill.setAliases(toJson(request.getAliases()));
        }
        if (StringUtils.hasText(request.getWhenToUse())) {
            skill.setWhenToUse(request.getWhenToUse());
        }
        if (StringUtils.hasText(request.getArgumentHint())) {
            skill.setArgumentHint(request.getArgumentHint());
        }
        if (request.getAllowedTools() != null) {
            skill.setAllowedTools(toJson(request.getAllowedTools()));
        }
        if (StringUtils.hasText(request.getModel())) {
            skill.setModel(request.getModel());
        }
        if (StringUtils.hasText(request.getExecutionContext())) {
            skill.setExecutionContext(ExecutionContext.fromValue(request.getExecutionContext()));
        }
        if (StringUtils.hasText(request.getEffort())) {
            skill.setEffort(SkillEffort.fromValue(request.getEffort()));
        }
        if (request.getDisableModelInvocation() != null) {
            skill.setDisableModelInvocation(request.getDisableModelInvocation() ? 1 : 0);
        }
        if (request.getUserInvocable() != null) {
            skill.setUserInvocable(request.getUserInvocable() ? 1 : 0);
        }
        if (request.getTags() != null) {
            skill.setTags(toJson(request.getTags()));
        }
    }

    // ── 统一内部方法 ─────────────────────────────────────────────────

    /**
     * 统一的保存逻辑。
     *
     * @param skillId       技能 UUID（新建时为新生成的 UUID）
     * @param newVersion    新版本号
     * @param creatorId     创建者 ID（新建时为 operatorId，更新时沿用原创建者）
     * @param creatorName   创建者名称
     * @param zipFile       ZIP 文件
     * @param request       请求体元数据
     * @param operatorId    操作者 ID
     * @param operatorName  操作者名称
     */
    private SkillRegisterResult doSave(String skillId, int newVersion,
                                        Long creatorId, String creatorName,
                                        MultipartFile zipFile, SkillRegisterRequest request,
                                        Long operatorId, String operatorName) {
        // 1. 校验 ZIP 文件
        SkillValidator.validateZipFile(zipFile);

        // 2. 解析 ZIP 获取 frontmatter 元数据
        ParsedSkillContent parsed = SkillContentParser.parseFromZip(zipFile);

        // 3. 用 request 元数据补充/覆盖（request 优先级高于 ZIP frontmatter）
        mergeRequestToParsed(parsed, request);

        // 4. 校验内容规则
        SkillValidator.validateSkillContent(parsed);

        // 5. 使用 operator 信息作为创建者（creatorId/creatorName 已在调用方处理）
        if (creatorId == null) {
            creatorId = operatorId;
        }
        if (creatorName == null) {
            creatorName = operatorName;
        }

        // 6. 上传文件到 Storage
        StorageInfoVO storageInfo = storageService.upload(skillId, newVersion, parsed.getFileMap());

        // 7. 构建并保存 SkillDO
        SkillDO skillDO = buildSkillDO(skillId, newVersion, parsed, storageInfo,
                creatorId, creatorName, operatorId, operatorName, request);
        skillStore.save(skillDO);

        log.info("[SkillRegisterService] Saved skill: skillId={}, version={}", skillId, newVersion);

        return SkillRegisterResult.builder()
                .skillId(skillId)
                .version(newVersion)
                .status(SkillStatus.DRAFT.getValue())
                .build();
    }

    // ── 兼容旧接口（保留用于其他模块调用）───────────────────────────────

    /**
     * ZIP 包方式注册（兼容接口）。
     * @deprecated 请使用 {@link #register(MultipartFile, SkillRegisterRequest, UserInfo)}
     */
    @Deprecated
    public SkillRegisterResult registerByZip(org.dragon.skill.dto.SkillZipRegisterRequest req,
                                              Long operatorId, String operatorName) {
        UserInfo user = new UserInfo(String.valueOf(operatorId), operatorName, null, true);
        return register(req.getZipFile(), new SkillRegisterRequest(), user);
    }

    /**
     * ZIP 包方式更新（兼容接口）。
     * @deprecated 请使用 {@link #update(String, MultipartFile, SkillRegisterRequest, UserInfo)}
     */
    @Deprecated
    public SkillRegisterResult updateByZip(String skillId,
                                           org.dragon.skill.dto.SkillZipRegisterRequest req,
                                           Long operatorId, String operatorName) {
        UserInfo user = new UserInfo(String.valueOf(operatorId), operatorName, null, true);
        return update(skillId, req.getZipFile(), new SkillRegisterRequest(), user);
    }

    // ── 私有辅助方法 ─────────────────────────────────────────────────

    /**
     * 用 request 元数据补充/覆盖 ParsedContent。
     *
     * <p>合并规则：
     * <ul>
     *   <li>request 的字段有值时，覆盖 parsed 中的对应字段</li>
     *   <li>request 的字段为空时，保留 parsed 中的原始值</li>
     * </ul>
     */
    private void mergeRequestToParsed(ParsedSkillContent parsed, SkillRegisterRequest request) {
        if (request == null) {
            return;
        }

        // 基本信息
        if (StringUtils.hasText(request.getName())) {
            parsed.setName(request.getName());
        }
        if (request.getDisplayName() != null) {
            parsed.setDisplayName(request.getDisplayName());
        }
        if (request.getDescription() != null) {
            parsed.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getContent())) {
            parsed.setBodyContent(request.getContent());
        }

        // 分类与可见性（visibility 常用作覆盖）
        if (StringUtils.hasText(request.getVisibility())) {
            parsed.setVisibility(request.getVisibility());
        }
        if (StringUtils.hasText(request.getCategory())) {
            parsed.setCategory(request.getCategory());
        }

        // 执行配置
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

        // 布尔配置
        if (request.getDisableModelInvocation() != null) {
            parsed.setDisableModelInvocation(request.getDisableModelInvocation());
        }
        if (request.getUserInvocable() != null) {
            parsed.setUserInvocable(request.getUserInvocable());
        }
    }

    /**
     * 组装 SkillDO 对象（不含 id、createdAt，由 DB 自动填充）。
     */
    private SkillDO buildSkillDO(String skillId, int version,
                                  ParsedSkillContent parsed, StorageInfoVO storageInfo,
                                  Long creatorId, String creatorName,
                                  Long editorId, String editorName,
                                  SkillRegisterRequest request) {
        SkillDO d = new SkillDO();
        d.setSkillId(skillId);
        d.setVersion(version);
        d.setName(parsed.getName());
        d.setDisplayName(parsed.getDisplayName());
        d.setDescription(parsed.getDescription());
        d.setContent(parsed.getBodyContent());
        d.setAliases(toJson(parsed.getAliases()));
        d.setWhenToUse(parsed.getWhenToUse());
        d.setArgumentHint(parsed.getArgumentHint());
        d.setAllowedTools(toJson(parsed.getAllowedTools()));
        d.setModel(parsed.getModel());
        d.setDisableModelInvocation(Boolean.TRUE.equals(parsed.getDisableModelInvocation()) ? 1 : 0);
        d.setUserInvocable(Boolean.FALSE.equals(parsed.getUserInvocable()) ? 0 : 1);
        d.setExecutionContext(ExecutionContext.fromValue(parsed.getExecutionContext()));
        d.setEffort(defaultIfNull(SkillEffort.fromValue(parsed.getEffort()), SkillEffort.AUTO));
        // tags 从 request 直接获取，不走 frontmatter 解析
        d.setTags(request != null && request.getTags() != null ? toJson(request.getTags()) : null);
        d.setCategory(defaultIfNull(SkillCategory.fromValue(parsed.getCategory()), SkillCategory.OTHER));
        d.setVisibility(SkillVisibility.fromValue(parsed.getVisibility()));
        d.setCreatorType(CreatorType.PERSONAL);
        d.setCreatorId(creatorId);
        d.setCreatorName(creatorName);
        d.setEditorId(editorId);
        d.setEditorName(editorName);
        d.setStatus(SkillStatus.DRAFT);  // 新版本始终为 DRAFT
        d.setPersist(0);  // 默认不持续留存
        d.setPersistMode(PersistMode.FULL);  // persist=1 时的默认模式
        d.setStorageType(storageInfo.getBucket() != null ? StorageType.S3 : StorageType.LOCAL);
        d.setStorageInfo(toJson(storageInfo));
        return d;
    }

    /** 查询最新版本，若不存在或已删除则抛出异常 */
    private SkillDO requireLatestNotDeleted(String skillId) {
        SkillDO latest = skillStore.findLatestBySkillId(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));
        if (SkillStatus.DELETED == latest.getStatus()) {
            throw new SkillNotFoundException(skillId);
        }
        return latest;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将 String userId 转换为 Long */
    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 日志
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillRegisterService.class);
}
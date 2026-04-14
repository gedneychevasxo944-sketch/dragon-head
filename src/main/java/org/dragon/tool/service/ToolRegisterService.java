package org.dragon.tool.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.permission.enums.ResourceType;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.domain.ToolStorageInfoVO;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.actionlog.ActionType;
import org.dragon.tool.enums.ToolStatus;
import org.dragon.tool.enums.ToolStorageType;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVersionStatus;
import org.dragon.tool.enums.ToolVisibility;
import org.dragon.tool.runtime.ToolChangeEvent;
import org.dragon.tool.runtime.adapter.UnifiedToolDeclaration;
import org.dragon.tool.store.ToolStore;
import org.dragon.tool.store.ToolVersionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 工具注册服务。
 *
 * <p>核心设计（对齐 SkillRegisterService）：
 * <ul>
 *   <li>{@link ToolDO}：工具元信息（不含版本内容）</li>
 *   <li>{@link ToolVersionDO}：工具版本内容（每次更新生成新版本，version 整数递增）</li>
 *   <li>注册：创建 ToolDO + V1 draft 版本</li>
 *   <li>更新：创建新 draft 版本（不改变已发布版本）</li>
 * </ul>
 *
 * <p><b>文件上传</b>：注册或更新工具时可附带 {@code fileMap}（相对路径 → 字节内容），
 * 本服务调用 {@link ToolFileService} 完成上传，并将返回的 {@link ToolStorageInfoVO}
 * 序列化写入 {@link ToolVersionDO#getStorageInfo()} 字段。
 * 不上传文件时传 {@code null} 或空 Map，{@code storageType / storageInfo} 字段保持 null。
 *
 * <p><b>注意</b>：ATOMIC 内建工具通过 {@code InMemoryToolStore.preload()} 预置，
 * MCP 工具通过 {@code McpToolLoader} 加载，均不经过本 Service。
 *
 * <p><b>与其他 Service 的边界</b>：
 * <ul>
 *   <li>状态流转（DRAFT→PUBLISHED 等）由 {@link ToolLifeCycleService} 负责</li>
 *   <li>workspace/character 绑定由 {@link ToolBindingService} 负责</li>
 *   <li>文件存储委托给 {@link ToolFileService}（local 或 S3 实现）</li>
 * </ul>
 */
@Service
public class ToolRegisterService {

    private static final Logger log = LoggerFactory.getLogger(ToolRegisterService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ToolStore toolStore;
    private final ToolVersionStore toolVersionStore;
    private final ToolActionLogService actionLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ToolFileService toolFileService;
    private final AssetMemberService assetMemberService;
    private final AssetPublishStatusService publishStatusService;

    /**
     * @param toolStore            工具持久化接口
     * @param toolVersionStore     版本持久化接口
     * @param actionLogService     操作日志服务
     * @param eventPublisher       Spring 事件发布器（用于缓存失效）
     * @param toolFileService      文件存储服务（local 或 S3 实现）
     * @param assetMemberService   资产成员服务（注册时写入 owner 关系）
     * @param publishStatusService 资产发布状态服务（注册时初始化为 DRAFT）
     */
    public ToolRegisterService(ToolStore toolStore,
                               ToolVersionStore toolVersionStore,
                               ToolActionLogService actionLogService,
                               ApplicationEventPublisher eventPublisher,
                               ToolFileService toolFileService,
                               AssetMemberService assetMemberService,
                               AssetPublishStatusService publishStatusService) {
        this.toolStore = Objects.requireNonNull(toolStore, "toolStore must not be null");
        this.toolVersionStore = Objects.requireNonNull(toolVersionStore, "toolVersionStore must not be null");
        this.actionLogService = Objects.requireNonNull(actionLogService, "actionLogService must not be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
        this.toolFileService = Objects.requireNonNull(toolFileService, "toolStorageService must not be null");
        this.assetMemberService = Objects.requireNonNull(assetMemberService, "assetMemberService must not be null");
        this.publishStatusService = Objects.requireNonNull(publishStatusService, "publishStatusService must not be null");
    }

    // ── 工具注册 ─────────────────────────────────────────────────────────

    /**
     * 注册一个新工具（用户自定义工具）。
     *
     * <p>调用方传入工具元信息和初始版本声明，本方法：
     * <ol>
     *   <li>生成 toolId（UUID）</li>
     *   <li>创建 ToolDO，初始状态为 {@link ToolStatus#DRAFT}</li>
     *   <li>若 fileMap 非空，调用 {@link ToolFileService#upload} 上传文件并填充存储信息</li>
     *   <li>创建 V1 {@link ToolVersionDO}，初始版本状态为 {@link ToolVersionStatus#DRAFT}</li>
     *   <li>需调用 {@link ToolLifeCycleService#publish(String, Long, Long, String, String)} 发布后才对 LLM 可见</li>
     * </ol>
     *
     * @param name            工具名称（全局唯一，LLM 通过此名发起 tool_call）
     * @param toolType        工具类型（HTTP / CODE / SKILL / COMPOSITE 等，不可为 ATOMIC）
     * @param visibility      可见性（PUBLIC = 无需绑定全局可用，通常为 PRIVATE）
     * @param declaration     工具声明（描述 + 参数 schema）
     * @param executionConfig 执行配置（各 ToolType 格式不同，参见 ToolVersionDO 注释）
     * @param fileMap         待上传文件（相对路径 → 字节内容），无文件时传 null 或空 Map
     * @param creatorId       创建者用户 ID
     * @param creatorName     创建者用户名
     * @return 注册成功后的工具 ID
     * @throws IllegalArgumentException 如果工具名已被注册，或 toolType 为 ATOMIC
     */
    public String registerTool(String name,
                               String displayName,
                               ToolType toolType,
                               ToolVisibility visibility,
                               UnifiedToolDeclaration declaration,
                               JsonNode executionConfig,
                               List<MultipartFile> files,
                               Long creatorId,
                               String creatorName) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(toolType, "toolType must not be null");
        Objects.requireNonNull(visibility, "visibility must not be null");
        Objects.requireNonNull(declaration, "declaration must not be null");

        if (toolType == ToolType.ATOMIC) {
            throw new IllegalArgumentException(
                    "ATOMIC tools are pre-loaded via InMemoryToolStore.preload(), not ToolRegisterService.");
        }

        String toolId = UUID.randomUUID().toString();
        int version = 1;
        LocalDateTime now = LocalDateTime.now();

        // 构建并保存 ToolDO
        ToolDO tool = ToolDO.builder()
                .id(toolId)
                .name(name)
                .displayName(displayName)
                .toolType(toolType)
                .visibility(visibility)
                .creatorId(creatorId)
                .creatorName(creatorName)
                .status(ToolStatus.DRAFT)
                .createdAt(now)
                .build();
        toolStore.save(tool);

        // 构建 V1 版本（含文件上传）
        ToolVersionDO initialVersion = buildVersionDO(toolId, version, toolType,
                declaration, executionConfig, files, creatorId, creatorName);
        toolVersionStore.save(initialVersion);

        // 注册资产所有者关系
        assetMemberService.addOwnerDirectly(ResourceType.TOOL, toolId, creatorId);
        // 初始化发布状态（默认为 DRAFT）
        publishStatusService.initializeStatus(ResourceType.TOOL, toolId, String.valueOf(creatorId));

        actionLogService.log(toolId, name, displayName, ActionType.TOOL_REGISTER, creatorName, version);

        log.info("[ToolRegisterService] 工具注册成功: toolId={}, name={}, type={}, visibility={}, fileCount={}",
                toolId, name, toolType, visibility, files == null ? 0 : files.size());

        return toolId;
    }

    /**
     * 注册工具（无文件上传）。
     */
    public String registerTool(String name,
                               String displayName,
                               ToolType toolType,
                               ToolVisibility visibility,
                               UnifiedToolDeclaration declaration,
                               JsonNode executionConfig,
                               Long creatorId,
                               String creatorName) {
        return registerTool(name, displayName, toolType, visibility, declaration,
                executionConfig, (List<MultipartFile>) null, creatorId, creatorName);
    }

    // ── 工具更新 ─────────────────────────────────────────────────────────

    /**
     * 更新已有工具（创建新 draft 版本，不改变已发布版本）。
     *
     * <p>新版本号 = 当前最大版本号 + 1，初始状态为 {@link ToolVersionStatus#DRAFT}。
     * 若 fileMap 非空，先上传文件并将存储信息写入新版本。
     * 需调用 {@link ToolLifeCycleService#publish(String, Long, Long, String, String)}  发布后才生效。
     *
     * @param toolId          工具 ID，必须已存在
     * @param declaration     新版本声明
     * @param executionConfig 新版本执行配置
     * @param fileMap         待上传文件（相对路径 → 字节内容），无文件时传 null 或空 Map
     * @param editorId        编辑者用户 ID
     * @param editorName      编辑者用户名
     * @return 新版本的 version 号
     * @throws IllegalArgumentException 如果工具 ID 不存在
     */
    public int updateTool(String toolId,
                          UnifiedToolDeclaration declaration,
                          JsonNode executionConfig,
                          List<MultipartFile> files,
                          Long editorId,
                          String editorName) {
        Objects.requireNonNull(toolId, "toolId must not be null");
        Objects.requireNonNull(declaration, "declaration must not be null");

        ToolDO tool = toolStore.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

        int newVersion = toolVersionStore.findMaxVersionByToolId(toolId) + 1;

        ToolVersionDO newVersionDO = buildVersionDO(toolId, newVersion, tool.getToolType(),
                declaration, executionConfig, files, editorId, editorName);
        toolVersionStore.save(newVersionDO);

        actionLogService.log(toolId, tool.getName(), tool.getDisplayName(),
                ActionType.TOOL_UPDATE, editorName, newVersion);

        log.info("[ToolRegisterService] 工具更新成功: toolId={}, newVersion={}, fileCount={}",
                toolId, newVersion, files == null ? 0 : files.size());

        return newVersion;
    }

    /**
     * 更新工具（无文件上传）。
     */
    public int updateTool(String toolId,
                          UnifiedToolDeclaration declaration,
                          JsonNode executionConfig,
                          Long editorId,
                          String editorName) {
        return updateTool(toolId, declaration, executionConfig, (List<MultipartFile>) null, editorId, editorName);
    }

    /**
     * 保存草稿（若已有 draft 版本则覆盖，否则新建）。
     *
     * <p>支持草稿文件的反复上传：若当前已有 draft 版本且版本号不变，
     * 新上传的文件会覆盖旧文件（LocalToolStorageService 使用 TRUNCATE_EXISTING 模式写入）。
     *
     * @param toolId          工具 ID
     * @param declaration     声明内容
     * @param executionConfig 执行配置
     * @param file            待上传文件，无文件时传 null
     * @param editorId        编辑者用户 ID
     * @param editorName      编辑者用户名
     * @return 草稿的 version 号
     */
    public int saveDraft(String toolId,
                         UnifiedToolDeclaration declaration,
                         JsonNode executionConfig,
                         List<MultipartFile> files,
                         Long editorId,
                         String editorName) {
        Objects.requireNonNull(toolId, "toolId must not be null");

        ToolDO tool = toolStore.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

        // 查找现有 draft 版本
        ToolVersionDO draftVersion = toolVersionStore.findDraftByToolId(toolId).orElse(null);

        int versionToReturn;
        if (draftVersion != null) {
            // 更新现有草稿（含可选文件上传）
            applyDeclaration(draftVersion, declaration);
            draftVersion.setExecutionConfig(executionConfig);
            draftVersion.setEditorId(editorId);
            draftVersion.setEditorName(editorName);
            if (hasFiles(files)) {
                applyStorageInfo(draftVersion, toolId, draftVersion.getVersion(), files);
            }
            toolVersionStore.update(draftVersion);
            versionToReturn = draftVersion.getVersion();
        } else {
            // 创建新草稿版本
            int newVersion = toolVersionStore.findMaxVersionByToolId(toolId) + 1;
            ToolVersionDO newVersionDO = buildVersionDO(toolId, newVersion, tool.getToolType(),
                    declaration, executionConfig, files, editorId, editorName);
            toolVersionStore.save(newVersionDO);
            versionToReturn = newVersion;
        }

        actionLogService.log(toolId, tool.getName(), tool.getDisplayName(),
                ActionType.TOOL_SAVE_DRAFT, editorName, versionToReturn);

        log.info("[ToolRegisterService] 草稿保存: toolId={}, version={}, fileCount={}",
                toolId, versionToReturn, files == null ? 0 : files.size());

        return versionToReturn;
    }

    /**
     * 保存草稿（无文件上传）。
     */
    public int saveDraft(String toolId,
                         UnifiedToolDeclaration declaration,
                         JsonNode executionConfig,
                         Long editorId,
                         String editorName) {
        return saveDraft(toolId, declaration, executionConfig, (List<MultipartFile>) null, editorId, editorName);
    }

    // ── 工具注销 ─────────────────────────────────────────────────────────

    /**
     * 注销指定 MCP Server 的所有工具（MCP Server 断连时调用）。
     *
     * <p>MCP 工具名格式为 {@code mcp__{serverName}__{toolName}}，
     * 注销后发布全量缓存失效事件，确保 ToolRegistry 缓存即时更新。
     *
     * @param mcpServerName MCP Server 名称
     */
    public void unregisterMcpServerTools(String mcpServerName) {
        Objects.requireNonNull(mcpServerName, "mcpServerName must not be null");
        // MCP 工具均在 InMemoryToolStore / DB 中，直接触发全量缓存失效
        // （具体从 ToolStore 中移除 MCP 工具的逻辑由 McpToolLoader 负责）
        eventPublisher.publishEvent(
                ToolChangeEvent.ofAll(this, "unregisterMcpServerTools: server=" + mcpServerName));
        log.info("[ToolRegisterService] MCP Server 工具批量注销缓存失效: server={}", mcpServerName);
    }

    // ── 内部辅助方法 ─────────────────────────────────────────────────────

    /**
     * 构建 ToolVersionDO（状态默认为 DRAFT），含可选文件上传。
     */
    private ToolVersionDO buildVersionDO(String toolId,
                                         int version,
                                         ToolType toolType,
                                         UnifiedToolDeclaration declaration,
                                         JsonNode executionConfig,
                                         List<MultipartFile> files,
                                         Long editorId,
                                         String editorName) {
        ToolVersionDO v = new ToolVersionDO();
        v.setToolId(toolId);
        v.setVersion(version);
        applyDeclaration(v, declaration);
        v.setExecutionConfig(executionConfig);
        v.setToolType(toolType);
        v.setEditorId(editorId);
        v.setEditorName(editorName);
        v.setStatus(ToolVersionStatus.DRAFT);
        v.setCreatedAt(LocalDateTime.now());

        // 文件上传（可选）
        if (hasFiles(files)) {
            applyStorageInfo(v, toolId, version, files);
        }

        return v;
    }

    /**
     * 将 {@link MultipartFile} 列表转为 fileMap 后调用 {@link ToolFileService} 上传，
     * 并将存储信息（含 type）写入版本对象。key 为原始文件名，重名文件后者覆盖前者。
     */
    private void applyStorageInfo(ToolVersionDO v,
                                  String toolId,
                                  int version,
                                  List<MultipartFile> files) {
        try {
            Map<String, byte[]> fileMap = new HashMap<>();
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                String filename = file.getOriginalFilename() != null
                        ? file.getOriginalFilename() : file.getName();
                fileMap.put(filename, file.getBytes());
            }
            ToolStorageInfoVO storageInfoVO = toolFileService.upload(toolId, version, fileMap);
            // 将 storageType 内聚到 VO 中，与 Skill 的 StorageInfo 保持一致
            ToolStorageType type = (storageInfoVO.getBucket() != null && !storageInfoVO.getBucket().isBlank())
                    ? ToolStorageType.S3 : ToolStorageType.LOCAL;
            storageInfoVO.setType(type);
            v.setStorageInfo(toJson(storageInfoVO));
            log.debug("[ToolRegisterService] 文件上传完成并写入版本: toolId={}, version={}, fileCount={}, type={}",
                    toolId, version, fileMap.size(), type);
        } catch (Exception e) {
            throw new IllegalStateException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 {@link UnifiedToolDeclaration} 的各属性写入 {@link ToolVersionDO} 的展开字段。
     */
    private void applyDeclaration(ToolVersionDO v, UnifiedToolDeclaration declaration) {
        v.setName(declaration.getName());
        v.setDescription(declaration.getDescription());
        v.setParameters(toJson(declaration.getParameters()));
        v.setRequiredParams(toJson(declaration.getRequired()));
        v.setAliases(toJson(declaration.getAliases()));
    }

    /**
     * 文件列表是否包含至少一个有效文件。
     */
    private boolean hasFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return false;
        return files.stream().anyMatch(f -> f != null && !f.isEmpty());
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize to JSON: " + obj, e);
        }
    }
}

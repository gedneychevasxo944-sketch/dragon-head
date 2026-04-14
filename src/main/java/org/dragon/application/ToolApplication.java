package org.dragon.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.permission.service.PermissionService;
import org.dragon.tool.domain.ToolDO;
import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.tool.dto.ToolActionLog;
import org.dragon.tool.dto.ToolRegisterRequest;
import org.dragon.tool.dto.ToolVO;
import org.dragon.tool.dto.ToolVersionVO;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.enums.ToolVisibility;
import org.dragon.tool.runtime.adapter.ParameterSchema;
import org.dragon.tool.runtime.adapter.UnifiedToolDeclaration;
import org.dragon.tool.service.ToolActionLogService;
import org.dragon.tool.service.ToolLifeCycleService;
import org.dragon.tool.service.ToolQueryService;
import org.dragon.tool.service.ToolRegisterService;
import org.dragon.tool.store.ToolStore;
import org.dragon.tool.store.ToolVersionStore;
import org.dragon.util.UserUtils;
import org.dragon.util.bean.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ToolApplication Tool 模块应用服务
 *
 * <p>对应前端 /tools 页面，聚合工具的查询、注册、版本管理等业务逻辑。
 * 核心操作委托给 {@code tool/} 目录下的各 Service 处理。
 *
 * @author ypf
 * @version 1.0
 */
@Slf4j
@Service
public class ToolApplication {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ToolStore toolStore;
    private final ToolVersionStore toolVersionStore;
    private final ToolQueryService toolQueryService;
    private final ToolRegisterService toolRegisterService;
    private final ToolLifeCycleService toolLifeCycleService;
    private final ToolActionLogService toolActionLogService;
    private final PermissionService permissionService;

    public ToolApplication(ToolStore toolStore,
                           ToolVersionStore toolVersionStore,
                           ToolQueryService toolQueryService,
                           ToolRegisterService toolRegisterService,
                           ToolLifeCycleService toolLifeCycleService,
                           ToolActionLogService toolActionLogService,
                           PermissionService permissionService) {
        this.toolStore = Objects.requireNonNull(toolStore);
        this.toolVersionStore = Objects.requireNonNull(toolVersionStore);
        this.toolQueryService = Objects.requireNonNull(toolQueryService);
        this.toolRegisterService = Objects.requireNonNull(toolRegisterService);
        this.toolLifeCycleService = Objects.requireNonNull(toolLifeCycleService);
        this.toolActionLogService = Objects.requireNonNull(toolActionLogService);
        this.permissionService = Objects.requireNonNull(permissionService);
    }

    // ==================== Tool CRUD ====================

    /**
     * 创建工具。
     *
     * @param files   附带文件列表（可为 null，CODE 类型工具可上传多个脚本文件）
     * @param request 工具注册请求
     * @return 工具详情
     */
    public ToolVO create(List<MultipartFile> files, ToolRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        Long userId = Long.parseLong(user.getUserId());

        ToolType toolType = parseEnum(ToolType.class, request.getToolType(), ToolType.HTTP);
        ToolVisibility visibility = parseEnum(ToolVisibility.class, request.getVisibility(), ToolVisibility.PRIVATE);

        UnifiedToolDeclaration declaration = buildDeclaration(request.getName(), request);
        JsonNode executionConfig = parseJsonNode(request.getExecutionConfig());

        String toolId = toolRegisterService.registerTool(
                request.getName(), request.getDisplayName(), toolType, visibility,
                declaration, executionConfig, files, userId, user.getUsername());

        log.info("[ToolApplication] Created tool: toolId={}", toolId);
        return getDetail(toolId);
    }

    /**
     * 更新工具（创建新 Draft 版本，不改变已发布版本）。
     *
     * @param toolId  工具 ID
     * @param files   附带文件列表（可为 null）
     * @param request 更新请求
     * @return 工具详情
     */
    public ToolVO update(String toolId, List<MultipartFile> files, ToolRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        Long userId = Long.parseLong(user.getUserId());

        ToolDO tool = toolStore.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

        UnifiedToolDeclaration declaration = buildDeclaration(tool.getName(), request);
        JsonNode executionConfig = parseJsonNode(request.getExecutionConfig());

        int newVersion = toolRegisterService.updateTool(
                toolId, declaration, executionConfig, files, userId, user.getUsername());

        log.info("[ToolApplication] Updated tool: toolId={}, newVersion={}", toolId, newVersion);
        return getDetail(toolId);
    }

    /**
     * 分页获取工具列表（含权限过滤）。
     *
     * @param page          第几页（从 1 开始）
     * @param pageSize      每页条数
     * @param search        关键词（匹配 name / introduction）
     * @param visibility    可见性筛选
     * @param toolType      类型筛选
     * @param runtimeStatus 运行状态筛选
     * @param category      分类（预留兼容）
     * @return 分页结果
     */
    public PageResponse<ToolVO> listTools(int page, int pageSize, String search,
                                          String visibility, String toolType,
                                          String runtimeStatus, String category) {
        List<ToolDO> tools = performSearch(search, visibility, toolType, runtimeStatus);

        // 按权限过滤（PUBLIC 工具对所有人可见，私有工具需有权限）
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.TOOL, userId);

        List<ToolVO> filtered = new ArrayList<>();
        for (ToolDO tool : tools) {
            if (tool.getVisibility() != ToolVisibility.PUBLIC) {
                if (visibleIds == null || !visibleIds.contains(tool.getId())) {
                    continue;
                }
            }
            filtered.add(toToolVO(tool));
        }

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<ToolVO> pageData = fromIndex >= filtered.size()
                ? List.of() : filtered.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 获取工具详情。
     *
     * @param toolId 工具 ID
     * @return 工具详情
     */
    public ToolVO getDetail(String toolId) {
        ToolDO tool = toolQueryService.getToolById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + toolId));

        ToolVO vo = toToolVO(tool);

        // 补充已发布版本的声明信息
        if (tool.getPublishedVersionId() != null) {
            toolVersionStore.findById(tool.getPublishedVersionId())
                    .ifPresent(v -> enrichWithVersion(vo, v));
        }

        // 补充关联关系
        List<String> workspaceIds = toolQueryService.getWorkspacesForTool(toolId);
        List<String> characterIds = toolQueryService.getCharactersForTool(toolId);

        if (!workspaceIds.isEmpty()) {
            vo.setWorkspaces(workspaceIds.stream()
                    .map(id -> ToolVO.WorkspaceRef.builder().id(id).build())
                    .toList());
        }
        if (!characterIds.isEmpty()) {
            vo.setCharacters(characterIds.stream()
                    .map(id -> ToolVO.CharacterRef.builder().id(id).build())
                    .toList());
        }

        return vo;
    }

    /**
     * 删除工具（软删除）。
     *
     * @param toolId 工具 ID
     */
    public void deleteTool(String toolId) {
        UserInfo user = UserUtils.getUserInfo();
        Long userId = Long.parseLong(user.getUserId());
        toolLifeCycleService.disable(toolId, userId, user.getUsername());
        log.info("[ToolApplication] Deleted tool: {}", toolId);
    }

    /**
     * 发布工具版本（DRAFT → PUBLISHED）。
     *
     * @param toolId      工具 ID
     * @param version     版本号
     * @param releaseNote 发版备注（可为 null）
     * @return 工具详情
     */
    public ToolVO publishTool(String toolId, int version, String releaseNote) {
        UserInfo user = UserUtils.getUserInfo();
        Long userId = Long.parseLong(user.getUserId());
        ToolVersionDO versionDO = toolVersionStore.findByToolIdAndVersion(toolId, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version not found: toolId=" + toolId + ", version=" + version));
        toolLifeCycleService.publish(toolId, versionDO.getId(), userId, user.getUsername(), releaseNote);
        log.info("[ToolApplication] Published tool: {} version: {}", toolId, version);
        return getDetail(toolId);
    }

    /**
     * 禁用工具。
     *
     * @param toolId 工具 ID
     */
    public void disableTool(String toolId) {
        UserInfo user = UserUtils.getUserInfo();
        toolLifeCycleService.disable(toolId, Long.parseLong(user.getUserId()), user.getUsername());
        log.info("[ToolApplication] Disabled tool: {}", toolId);
    }

    /**
     * 启用工具。
     *
     * @param toolId 工具 ID
     */
    public void enableTool(String toolId) {
        UserInfo user = UserUtils.getUserInfo();
        toolLifeCycleService.enable(toolId, Long.parseLong(user.getUserId()), user.getUsername());
        log.info("[ToolApplication] Enabled tool: {}", toolId);
    }

    /**
     * 获取工具版本列表。
     *
     * @param toolId 工具 ID
     * @return 版本列表
     */
    public List<ToolVersionVO> listVersions(String toolId) {
        return toolVersionStore.findAllByToolId(toolId).stream()
                .map(v -> ToolVersionVO.builder()
                        .version(v.getVersion())
                        .versionStatus(v.getStatus())
                        .editorId(v.getEditorId())
                        .editorName(v.getEditorName())
                        .createdAt(v.getCreatedAt())
                        .publishedAt(v.getPublishedAt())
                        .releaseNote(v.getReleaseNote())
                        .build())
                .toList();
    }

    /**
     * 获取工具操作日志（分页）。
     *
     * @param toolId 工具 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页结果
     */
    public PageResponse<ToolActionLog> getActionLogs(String toolId, int page, int size) {
        return toolActionLogService.pageByTool(toolId, page, size);
    }

    // ── 私有辅助方法 ──────────────────────────────────────────────────

    /**
     * 执行条件查询（内存过滤）。
     */
    private List<ToolDO> performSearch(String search, String visibility,
                                       String toolType, String runtimeStatus) {
        List<ToolDO> all = toolStore.findAll();

        List<ToolDO> result = new ArrayList<>();
        for (ToolDO tool : all) {
            // 关键词过滤
            if (search != null && !search.isBlank()) {
                String s = search.toLowerCase();
                boolean nameMatch = tool.getName() != null && tool.getName().toLowerCase().contains(s);
                boolean introMatch = tool.getIntroduction() != null
                        && tool.getIntroduction().toLowerCase().contains(s);
                if (!nameMatch && !introMatch) continue;
            }
            // 可见性过滤
            if (visibility != null && !visibility.isBlank() && !"all".equalsIgnoreCase(visibility)) {
                if (tool.getVisibility() == null || !visibility.equalsIgnoreCase(tool.getVisibility().name())) {
                    continue;
                }
            }
            // 类型过滤
            if (toolType != null && !toolType.isBlank() && !"all".equalsIgnoreCase(toolType)) {
                if (tool.getToolType() == null || !toolType.equalsIgnoreCase(tool.getToolType().name())) {
                    continue;
                }
            }
            // 状态过滤
            if (runtimeStatus != null && !runtimeStatus.isBlank() && !"all".equalsIgnoreCase(runtimeStatus)) {
                if (tool.getStatus() == null || !runtimeStatus.equalsIgnoreCase(tool.getStatus().name())) {
                    continue;
                }
            }
            result.add(tool);
        }
        return result;
    }

    /**
     * 将 {@link ToolDO} 转换为 {@link ToolVO}。
     */
    private ToolVO toToolVO(ToolDO tool) {
        return ToolVO.builder()
                .id(tool.getId())
                .name(tool.getName())
                .displayName(tool.getDisplayName())
                .introduction(tool.getIntroduction())
                .toolType(tool.getToolType())
                .visibility(tool.getVisibility())
                .builtin(tool.isBuiltin())
                .tags(parseTagsList(tool.getTags()))
                .creatorId(tool.getCreatorId())
                .creatorName(tool.getCreatorName())
                .status(tool.getStatus())
                .createdAt(tool.getCreatedAt())
                .build();
    }

    /**
     * 将 {@link ToolVersionDO} 的声明字段补充到 {@link ToolVO} 中（用于详情展示）。
     */
    private void enrichWithVersion(ToolVO vo, ToolVersionDO version) {
        vo.setDescription(version.getDescription());
        vo.setVersion(version.getVersion());
        vo.setVersionStatus(version.getStatus());
        vo.setEditorId(version.getEditorId());
        vo.setEditorName(version.getEditorName());
        vo.setPublishedAt(version.getPublishedAt());
    }

    /**
     * 从请求数据中构建 {@link UnifiedToolDeclaration}。
     */
    private UnifiedToolDeclaration buildDeclaration(String name, ToolRegisterRequest request) {
        String description = request.getDescription() != null ? request.getDescription() : "";

        Map<String, ParameterSchema> parameters = new java.util.HashMap<>();
        if (request.getParameters() != null) {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(request.getParameters());
                Map<String, ParameterSchema> parsed = OBJECT_MAPPER.readValue(json,
                        OBJECT_MAPPER.getTypeFactory().constructMapType(
                                java.util.HashMap.class, String.class, ParameterSchema.class));
                if (parsed != null) {
                    parameters = parsed;
                }
            } catch (Exception e) {
                log.warn("[ToolApplication] 解析 parameters 失败，忽略: {}", e.getMessage());
            }
        }

        UnifiedToolDeclaration.UnifiedToolDeclarationBuilder builder = UnifiedToolDeclaration.builder()
                .name(name)
                .description(description);
        for (Map.Entry<String, ParameterSchema> entry : parameters.entrySet()) {
            builder.parameter(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    /**
     * 将任意对象解析为 {@link JsonNode}（允许为 null）。
     */
    private JsonNode parseJsonNode(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof String s) {
                return OBJECT_MAPPER.readTree(s);
            }
            return OBJECT_MAPPER.valueToTree(obj);
        } catch (Exception e) {
            log.warn("[ToolApplication] 解析 executionConfig 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 JSON 字符串形式的 tags 解析为 List（解析失败时返回 null）。
     */
    private List<String> parseTagsList(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(tagsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("[ToolApplication] 解析 tags 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 安全解析枚举，解析失败时返回默认值。
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[ToolApplication] 枚举解析失败: class={}, value={}, 使用默认值={}",
                    enumClass.getSimpleName(), value, defaultValue);
            return defaultValue;
        }
    }
}

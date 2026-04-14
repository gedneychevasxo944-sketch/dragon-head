package org.dragon.tool.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.actionlog.ActionLogStore;
import org.dragon.actionlog.ActionType;
import org.dragon.observer.log.ObserverActionLog;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.tool.dto.ToolActionLog;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工具操作日志服务。
 *
 * <p>底层使用项目统一的 {@code observer_action_log} 表（targetType="TOOL"），
 * 不再维护独立的 tool_action_log 表。
 *
 * <p>details 字段结构（扁平 Map）：
 * <pre>
 * {
 *   "name":          工具名称（ToolDO.name）,
 *   "version":       版本号（Integer，无版本操作时不写入）,
 *   // 各操作类型的扩展字段（由调用方直接传入 extra Map）：
 *   "releaseNote":   发版备注（PUBLISH）,
 *   "characterId":   Character ID（BIND_CHARACTER / UNBIND）,
 *   "characterName": Character 名称（BIND_CHARACTER / UNBIND）,
 *   "workspaceId":   Workspace ID（BIND_WORKSPACE / UNBIND）,
 *   "workspaceName": Workspace 名称（BIND_WORKSPACE / UNBIND）,
 *   "scopeType":     解绑类型（UNBIND：character / workspace）
 * }
 * </pre>
 */
@Slf4j
@Service
public class ToolActionLogService {

    private static final String TARGET_TYPE = "TOOL";

    private final ActionLogStore actionLogStore;

    public ToolActionLogService(StoreFactory storeFactory) {
        this.actionLogStore = storeFactory.get(ActionLogStore.class);
    }

    // ── 记录日志 ──────────────────────────────────────────────────────

    /**
     * 记录操作日志（含扩展字段）。
     *
     * @param toolId       工具 ID
     * @param name         工具名称
     * @param displayName  工具展示名称（可为 null）
     * @param actionType   操作类型（项目统一 ActionType）
     * @param operatorName 操作人名称
     * @param version      版本号（无版本的操作传 null）
     * @param extra        操作类型相关的扩展字段（可为 null）
     */
    public void log(String toolId, String name, String displayName,
                    ActionType actionType, String operatorName,
                    Integer version, Map<String, Object> extra) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", name);
        if (displayName != null) details.put("displayName", displayName);
        if (version != null) details.put("version", version);
        if (extra != null) details.putAll(extra);

        ObserverActionLog actionLog = new ObserverActionLog(
                TARGET_TYPE, toolId, actionType, operatorName, details);
        actionLog.setId(UUID.randomUUID().toString());
        actionLog.setCreatedAt(LocalDateTime.now());

        actionLogStore.save(actionLog);
        log.debug("[ToolActionLogService] Logged: toolId={}, action={}, operator={}",
                toolId, actionType, operatorName);
    }

    /**
     * 记录操作日志（无扩展字段）。
     */
    public void log(String toolId, String name, String displayName,
                    ActionType actionType, String operatorName, Integer version) {
        log(toolId, name, displayName, actionType, operatorName, version, null);
    }

    // ── 查询日志 ──────────────────────────────────────────────────────

    /**
     * 分页查询某工具的操作日志。
     *
     * @param toolId 工具 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @return 分页结果
     */
    public PageResponse<ToolActionLog> pageByTool(String toolId, int page, int size) {
        int offset = (page - 1) * size;
        List<ObserverActionLog> logs = actionLogStore.findByTarget(TARGET_TYPE, toolId, offset, size);
        int total = actionLogStore.countByTarget(TARGET_TYPE, toolId);

        List<ToolActionLog> items = logs.stream()
                .map(this::toVO)
                .toList();

        return PageResponse.of(items, total, page, size);
    }

    // ── 私有：ObserverActionLog → ToolActionLogVO ────────────────────

    /**
     * 在 tool 模块内完成 ObserverActionLog → ToolActionLogVO 的转换，
     * 直接从 details Map 按 key 读取，避免泛型反序列化问题。
     */
    private ToolActionLog toVO(ObserverActionLog actionLog) {
        Map<String, Object> d = actionLog.getDetails() != null ? actionLog.getDetails() : Map.of();

        ActionType actionType = null;
        if (actionLog.getActionType() != null) {
            try {
                actionType = actionLog.getActionType();
            } catch (Exception ignored) {
            }
        }

        return ToolActionLog.builder()
                .id(actionLog.getId())
                .toolId(actionLog.getTargetId())
                .name((String) d.get("name"))
                .displayName((String) d.get("displayName"))
                .actionType(actionType)
                .actionLabel(actionType != null ? actionType.name() : null)
                .version(d.get("version") != null ? ((Number) d.get("version")).intValue() : null)
                .operatorName(actionLog.getOperator())
                .content(buildContent(actionType, d))
                .createdAt(actionLog.getCreatedAt())
                .build();
    }

    /**
     * 根据 actionType 和 details 生成可读的操作描述。
     */
    private String buildContent(ActionType actionType, Map<String, Object> d) {
        if (actionType == null) return "";
        return switch (actionType) {
            case TOOL_PUBLISH -> {
                String note = (String) d.get("releaseNote");
                Integer ver = d.get("version") != null ? ((Number) d.get("version")).intValue() : null;
                String base = ver != null ? "发布版本 v" + ver : "发布版本";
                yield (note != null && !note.isBlank()) ? base + ": " + note : base;
            }
            case TOOL_BIND_CHARACTER -> "绑定到 Character [" + d.get("characterName") + "]";
            case TOOL_BIND_WORKSPACE -> "绑定到 Workspace [" + d.get("workspaceName") + "]";
            case TOOL_UNBIND -> {
                String type = (String) d.get("scopeType");
                yield switch (type != null ? type : "") {
                    case "character" -> "解除绑定 Character [" + d.get("characterName") + "]";
                    case "workspace" -> "解除绑定 Workspace [" + d.get("workspaceName") + "]";
                    default -> "解除绑定";
                };
            }
            default -> actionType.name();
        };
    }
}

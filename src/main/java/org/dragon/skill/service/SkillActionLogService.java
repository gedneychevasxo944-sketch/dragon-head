package org.dragon.skill.service;

import lombok.extern.slf4j.Slf4j;
import org.dragon.actionlog.ActionLogStore;
import org.dragon.actionlog.ActionType;
import org.dragon.observer.log.ObserverActionLog;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.skill.dto.SkillActionLog;
import org.dragon.store.StoreFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Skill 操作日志服务。
 *
 * <p>底层使用项目统一的 {@code observer_action_log} 表（targetType="SKILL"），
 * 不再维护独立的 skill_action_log 表。
 *
 * <p>details 字段结构（扁平 Map）：
 * <pre>
 * {
 *   "name":        技能 name（SkillDO.name）,
 *   "displayName": 技能展示名（SkillDO.displayName，可为 null）,
 *   "version":     版本号（Integer，无版本操作时不写入）,
 *   // 各操作类型的扩展字段（由调用方直接传入 extra Map）：
 *   "releaseNote":   发版备注（PUBLISH）,
 *   "characterId":   Character ID（BIND_CHARACTER / UNBIND）,
 *   "characterName": Character 名称（BIND_CHARACTER / UNBIND）,
 *   "workspaceId":   Workspace ID（BIND_WORKSPACE / UNBIND）,
 *   "workspaceName": Workspace 名称（BIND_WORKSPACE / UNBIND）,
 *   "bindingType":   解绑类型（UNBIND：character / workspace）
 * }
 * </pre>
 */
@Slf4j
@Service
public class SkillActionLogService {

    private static final String TARGET_TYPE = "SKILL";

    private final ActionLogStore actionLogStore;

    public SkillActionLogService(StoreFactory storeFactory) {
        this.actionLogStore = storeFactory.get(ActionLogStore.class);
    }

    // ── 记录日志 ──────────────────────────────────────────────────────

    /**
     * 记录操作日志。
     *
     * @param skillId      技能 ID
     * @param name         技能 name（SkillDO.name）
     * @param displayName  技能展示名（SkillDO.displayName，可为 null）
     * @param actionType   操作类型（项目统一 ActionType）
     * @param operatorName 操作人名称
     * @param version      版本号（无版本的操作传 null）
     * @param extra        操作类型相关的扩展字段（可为 null）
     */
    public void log(String skillId, String name, String displayName,
                    ActionType actionType, String operatorName,
                    Integer version, Map<String, Object> extra) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", name);
        if (displayName != null) details.put("displayName", displayName);
        if (version != null) details.put("version", version);
        if (extra != null) details.putAll(extra);

        ObserverActionLog actionLog = new ObserverActionLog(
                TARGET_TYPE, skillId, actionType, operatorName, details);
        actionLog.setId(UUID.randomUUID().toString());
        actionLog.setCreatedAt(LocalDateTime.now());

        actionLogStore.save(actionLog);
        log.debug("[SkillActionLogService] Logged: skillId={}, action={}, operator={}",
                skillId, actionType, operatorName);
    }

    /**
     * 记录操作日志（无扩展字段）。
     */
    public void log(String skillId, String name, String displayName,
                    ActionType actionType, String operatorName, Integer version) {
        log(skillId, name, displayName, actionType, operatorName, version, null);
    }

    // ── 查询日志 ──────────────────────────────────────────────────────

    /**
     * 分页查询某 Skill 的操作日志。
     *
     * @param skillId 技能 ID
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @return 分页结果
     */
    public PageResponse<SkillActionLog> pageBySkill(String skillId, int page, int size) {
        int offset = (page - 1) * size;
        List<ObserverActionLog> logs = actionLogStore.findByTarget(TARGET_TYPE, skillId, offset, size);
        int total = actionLogStore.countByTarget(TARGET_TYPE, skillId);

        List<SkillActionLog> items = logs.stream()
                .map(this::toVO)
                .toList();

        return PageResponse.of(items, total, page, size);
    }

    // ── 私有：ObserverActionLog → SkillActionLogVO ───────────────────

    /**
     * 在 skill 模块内完成 ObserverActionLog → SkillActionLogVO 的转换，
     * 直接从 details Map 按 key 读取，避免泛型反序列化问题。
     */
    private SkillActionLog toVO(ObserverActionLog log) {
        Map<String, Object> d = log.getDetails() != null ? log.getDetails() : Map.of();

        ActionType actionType = null;
        if (log.getActionType() != null) {
            try {
                actionType = log.getActionType();
            } catch (Exception ignored) {
            }
        }

        return SkillActionLog.builder()
                .id(log.getId())
                .skillId(log.getTargetId())
                .name((String) d.get("name"))
                .displayName((String) d.get("displayName"))
                .actionType(actionType)
                .actionLabel(actionType != null ? actionType.name() : null)
                .version(d.get("version") != null ? ((Number) d.get("version")).intValue() : null)
                .operatorName(log.getOperator())
                .content(buildContent(actionType, d))
                .createdAt(log.getCreatedAt())
                .build();
    }

    /**
     * 根据 actionType 和 details 生成可读的操作描述。
     */
    private String buildContent(ActionType actionType, Map<String, Object> d) {
        if (actionType == null) return "";
        return switch (actionType) {
            case SKILL_PUBLISH -> {
                String note = (String) d.get("releaseNote");
                yield (note != null && !note.isBlank()) ? "发布版本: " + note : "发布版本";
            }
            case SKILL_BIND_CHARACTER -> "绑定到 Character [" + d.get("characterName") + "]";
            case SKILL_BIND_WORKSPACE -> "绑定到 Workspace [" + d.get("workspaceName") + "]";
            case SKILL_UNBIND -> {
                String type = (String) d.get("bindingType");
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

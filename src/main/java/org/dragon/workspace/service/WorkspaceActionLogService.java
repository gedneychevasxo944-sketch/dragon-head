package org.dragon.workspace.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.observer.actionlog.ActionType;
import org.dragon.observer.actionlog.ObserverActionLog;
import org.dragon.observer.actionlog.ObserverActionLogService;

/**
 * WorkspaceActionLogService Workspace 行为日志服务
 * 统一记录 Workspace 内所有行为（Character 行为、Character 间交互、任务行为、雇佣行为等）
 *
 * @deprecated 请使用 {@link ObserverActionLogService} 代替
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated
public class WorkspaceActionLogService {

    private final ObserverActionLogService observerActionLogService;

    /**
     * 记录动作日志
     *
     * @deprecated 请使用 {@link ObserverActionLogService} 代替
     */
    @Deprecated
    public void logAction(String workspaceId, ActionType actionType,
            String targetCharacterId, String operator, String details) {
        // 构建详细信息
        Map<String, Object> observerDetails = Map.of(
                "workspaceId", workspaceId != null ? workspaceId : "",
                "details", details != null ? details : ""
        );

        // 上报到 ObserverActionLogService
        observerActionLogService.logAction("CHARACTER", targetCharacterId, actionType, operator, observerDetails);

        log.info("[WorkspaceActionLogService] Logged action: {} for character: {} in workspace: {}",
                actionType.name(), targetCharacterId, workspaceId);
    }

    /**
     * 获取 Workspace 的所有动作日志
     *
     * @deprecated 请使用 {@link ObserverActionLogService#getActionLogs(String, String)} 代替
     */
    @Deprecated
    public List<ObserverActionLog> getActionLogs(String workspaceId) {
        return observerActionLogService.getActionLogs("WORKSPACE", workspaceId);
    }

    /**
     * 根据 Character 获取动作日志
     *
     * @deprecated 请使用 {@link ObserverActionLogService#getActionLogs(String, String)} 代替
     */
    @Deprecated
    public List<ObserverActionLog> getActionLogsByCharacter(String workspaceId, String characterId) {
        return observerActionLogService.getActionLogs("CHARACTER", characterId);
    }

    /**
     * 根据动作类型获取动作日志
     *
     * @deprecated 请使用 {@link ObserverActionLogService#getActionLogsByType(ActionType)} 代替
     */
    @Deprecated
    public List<ObserverActionLog> getActionLogsByType(String workspaceId, ActionType actionType) {
        return observerActionLogService.getActionLogsByType(actionType);
    }
}

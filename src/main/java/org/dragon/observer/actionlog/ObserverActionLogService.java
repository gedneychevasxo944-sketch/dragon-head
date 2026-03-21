package org.dragon.observer.actionlog;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Observer 动作日志服务
 * 提供统一的事件上报接口
 *
 * @author wyj
 * @version 1.0
 */
@Service
public class ObserverActionLogService {

    private static final Logger log = LoggerFactory.getLogger(ObserverActionLogService.class);

    private final ActionLogStore actionLogStore;

    public ObserverActionLogService(ActionLogStore actionLogStore) {
        this.actionLogStore = actionLogStore;
    }

    /**
     * 记录通用动作日志
     *
     * @param targetType  目标类型 (如 WORKSPACE, CHARACTER, TASK 等)
     * @param targetId    目标ID
     * @param actionType  动作类型
     * @param operator    操作者
     * @param details     详细信息
     */
    public void logAction(String targetType, String targetId, ActionType actionType,
                          String operator, Map<String, Object> details) {
        ObserverActionLog actionLog = new ObserverActionLog(
                targetType, targetId, actionType, operator, details);
        actionLogStore.save(actionLog);
        log.info("[ObserverActionLogService] Logged action: {} - {} - {} for target: {}",
                targetType, actionType, operator, targetId);
    }

    /**
     * 记录 LLM 调用日志
     *
     * @param targetType     目标类型
     * @param targetId       目标ID
     * @param model          模型名称
     * @param input          输入
     * @param output         输出
     * @param inputTokens    输入 token 数
     * @param outputTokens   输出 token 数
     * @param latencyMs      延迟（毫秒）
     * @param success        是否成功
     */
    public void logLLMCall(String targetType, String targetId, String model,
                           String input, String output, int inputTokens, int outputTokens,
                           long latencyMs, boolean success) {
        ActionType actionType = success ? ActionType.LLM_CALL_COMPLETE : ActionType.LLM_CALL_FAIL;

        Map<String, Object> details = Map.of(
                "model", model != null ? model : "",
                "input", input != null ? input : "",
                "output", output != null ? output : "",
                "inputTokens", inputTokens,
                "outputTokens", outputTokens,
                "latencyMs", latencyMs,
                "success", success
        );

        logAction(targetType, targetId, actionType, "system", details);
    }

    /**
     * 记录 LLM 调用开始
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @param model      模型名称
     * @param input      输入
     */
    public void logLLMCallStart(String targetType, String targetId, String model, String input) {
        Map<String, Object> details = Map.of(
                "model", model != null ? model : "",
                "input", input != null ? input : ""
        );
        logAction(targetType, targetId, ActionType.LLM_CALL_START, "system", details);
    }

    /**
     * 记录 LLM 调用结束
     *
     * @param targetType     目标类型
     * @param targetId       目标ID
     * @param model          模型名称
     * @param output         输出
     * @param inputTokens    输入 token 数
     * @param outputTokens   输出 token 数
     * @param latencyMs      延迟（毫秒）
     */
    public void logLLMCallEnd(String targetType, String targetId, String model,
                              String output, int inputTokens, int outputTokens, long latencyMs) {
        Map<String, Object> details = Map.of(
                "model", model != null ? model : "",
                "output", output != null ? output : "",
                "inputTokens", inputTokens,
                "outputTokens", outputTokens,
                "latencyMs", latencyMs
        );
        logAction(targetType, targetId, ActionType.LLM_CALL_COMPLETE, "system", details);
    }

    /**
     * 记录任务状态变更
     *
     * @param taskId     任务ID
     * @param fromStatus 原状态
     * @param toStatus   新状态
     * @param operator   操作者
     */
    public void logTaskStatusChange(String taskId, String fromStatus, String toStatus, String operator) {
        Map<String, Object> details = Map.of(
                "fromStatus", fromStatus != null ? fromStatus : "",
                "toStatus", toStatus != null ? toStatus : ""
        );
        logAction("TASK", taskId, ActionType.TASK_CREATE, operator, details);
    }

    /**
     * 记录工作空间事件
     *
     * @param workspaceId  工作空间ID
     * @param actionType  动作类型
     * @param operator    操作者
     * @param details     详细信息
     */
    public void logWorkspaceEvent(String workspaceId, ActionType actionType,
                                  String operator, Map<String, Object> details) {
        logAction("WORKSPACE", workspaceId, actionType, operator, details);
    }

    /**
     * 记录雇佣事件
     *
     * @param workspaceId  工作空间ID
     * @param characterId Character ID
     * @param actionType  动作类型 (HIRE/FIRE/UPDATE_DUTY/AUTO_ASSIGN)
     * @param operator    操作者
     * @param details     详细信息
     */
    public void logHireEvent(String workspaceId, String characterId, ActionType actionType,
                             String operator, Map<String, Object> details) {
        logAction("CHARACTER", characterId, actionType, operator, details);
    }

    /**
     * 记录 Character 执行事件
     *
     * @param characterId Character ID
     * @param actionType   动作类型
     * @param details     详细信息
     */
    public void logCharacterEvent(String characterId, ActionType actionType, Map<String, Object> details) {
        logAction("CHARACTER", characterId, actionType, "system", details);
    }

    /**
     * 记录任务编排事件
     *
     * @param taskId     任务ID
     * @param actionType 动作类型
     * @param operator   操作者
     * @param details    详细信息
     */
    public void logTaskArrangementEvent(String taskId, ActionType actionType,
                                        String operator, Map<String, Object> details) {
        logAction("TASK", taskId, actionType, operator, details);
    }

    /**
     * 查询目标的所有日志
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 日志列表
     */
    public List<ObserverActionLog> getActionLogs(String targetType, String targetId) {
        return actionLogStore.findByTarget(targetType, targetId);
    }

    /**
     * 查询指定动作类型的日志
     *
     * @param actionType 动作类型
     * @return 日志列表
     */
    public List<ObserverActionLog> getActionLogsByType(ActionType actionType) {
        return actionLogStore.findByActionType(actionType);
    }

    /**
     * 查询所有日志
     *
     * @return 日志列表
     */
    public List<ObserverActionLog> getAllActionLogs() {
        return actionLogStore.findAll();
    }

    /**
     * 清空所有日志
     */
    public void clearAll() {
        actionLogStore.clear();
    }
}

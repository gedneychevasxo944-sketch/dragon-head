package org.dragon.agent.react;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * ReAct 动作
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Action {

    /**
     * 动作类型
     */
    private ActionType type;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 参数
     */
    private Map<String, Object> parameters;

    /**
     * 指定执行该动作使用的模型 (可选)
     */
    private String modelId;

    /**
     * 响应内容 (用于 FINISH/RESPOND 类型)
     */
    private String response;

    /**
     * 状态变更 (用于 STATUS_CHANGE 类型)
     */
    private StatusChange statusChange;

    /**
     * 动作类型枚举
     */
    public enum ActionType {
        /** 调用工具 */
        TOOL,
        /** 查询记忆 */
        MEMORY,
        /** 生成回复 */
        RESPOND,
        /** 结束执行 */
        FINISH,
        /** 状态变更 */
        STATUS_CHANGE
    }

    /**
     * 状态变更结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusChange {
        /**
         * 目标状态
         */
        private String targetStatus;

        /**
         * 变更原因
         */
        private String reason;

        /**
         * 等待的依赖任务 ID（WAITING_DEPENDENCY 时填写）
         */
        private String dependencyTaskId;

        /**
         * 需要用户回答的问题（WAITING_USER_INPUT 时填写）
         */
        private String question;
    }
}

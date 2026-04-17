package org.dragon.task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task 任务实体
 * 作为基础包，统一表示所有任务
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    // ==================== 核心标识 ====================

    /**
     * 任务唯一标识
     */
    private String id;

    /**
     * 工作空间 ID
     */
    private String workspaceId;

    /**
     * 父任务 ID（如果是子任务则有值）
     */
    private String parentTaskId;

    /**
     * 创建者 ID
     */
    private String creatorId;

    /**
     * 执行者 Character ID
     */
    private String characterId;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    // ==================== 执行状态 ====================

    /**
     * 执行状态
     */
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 开始执行时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 执行模式
     */
    private String executionMode;

    /**
     * 工作流 ID
     */
    private String workflowId;

    /**
     * 等待原因
     */
    private String waitingReason;

    /**
     * 任务结果（String 形式）
     */
    private String result;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 当前流式消息（正在输出中）
     */
    private String currentStreamingContent;

    /**
     * 执行步骤列表
     */
    @Builder.Default
    private List<ExecutionStep> executionSteps = new ArrayList<>();

    /**
     * 执行消息列表（支持流式）
     */
    @Builder.Default
    private List<ExecutionMessage> executionMessages = new ArrayList<>();

    // ==================== 来源上下文 ====================

    /**
     * 源消息 ID
     */
    private String sourceMessageId;

    /**
     * 源聊天 ID
     */
    private String sourceChatId;

    /**
     * 源渠道
     */
    private String sourceChannel;

    // ==================== 协作上下文 ====================

    /**
     * 分配的成员 ID 列表
     */
    @Builder.Default
    private List<String> assignedMemberIds = new ArrayList<>();

    /**
     * 依赖任务 ID 列表
     */
    @Builder.Default
    private List<String> dependencyTaskIds = new ArrayList<>();

    // ==================== 需求协作字段 ====================

    /**
     * 发布需求的原始 Character（无人认领时交回）
     */
    private String originalCharacterId;

    /**
     * 当前认领者列表（开放需求时，多个认领者）
     */
    @Builder.Default
    private List<String> claimerIds = new ArrayList<>();

    /**
     * 指定执行者（指定需求时，只有此执行者完成才唤醒）
     */
    private String waitingForCharacterId;

    // ==================== 认领者管理 ====================

    /**
     * 添加认领者
     */
    public void addClaimerId(String characterId) {
        if (this.claimerIds == null) {
            this.claimerIds = new ArrayList<>();
        }
        if (!this.claimerIds.contains(characterId)) {
            this.claimerIds.add(characterId);
        }
    }

    /**
     * 移除认领者
     */
    public void removeClaimerId(String characterId) {
        if (this.claimerIds != null) {
            this.claimerIds.remove(characterId);
        }
    }

    /**
     * 最后的问题（用于追问用户）
     */
    private String lastQuestion;

    // ==================== 数据 & 扩展 ====================

    /**
     * 任务输入
     */
    private Object input;

    /**
     * 任务输出
     */
    private Object output;

    /**
     * 子任务 ID 列表
     */
    @Builder.Default
    private List<String> childTaskIds = new ArrayList<>();

    /**
     * 关联的物料 ID 列表
     */
    private List<String> materialIds;

    /**
     * 交互上下文
     */
    private Object interactionContext;

    /**
     * 任务元数据
     */
    private Map<String, Object> metadata;

    /**
     * 任务扩展属性
     */
    private Map<String, Object> extensions;

    // ==================== 生命周期 ====================

    /**
     * 恢复令牌
     */
    private String resumeToken;

    /**
     * 恢复上下文
     */
    private Map<String, Object> resumeContext;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ==================== 嵌套类 ====================

    /**
     * 执行状态
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskExecutionState {
        private TaskStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private String executionMode;
        private String workflowId;
        private String waitingReason;
        private String result;
        private String errorMessage;
        private String currentStreamingContent;
        private List<ExecutionStep> executionSteps;
        private List<ExecutionMessage> executionMessages;
    }

    /**
     * 来源上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSourceContext {
        private String sourceMessageId;
        private String sourceChatId;
        private String sourceChannel;
    }

    /**
     * 协作上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskCollaborationContext {
        private List<String> assignedMemberIds;
        private List<String> dependencyTaskIds;
        private String lastQuestion;
    }

    /**
     * ExecutionStep 执行步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep {

        /**
         * 步骤序号
         */
        @Builder.Default
        private Integer stepNumber = 0;

        /**
         * 步骤类型 (THOUGHT, ACTION, OBSERVATION)
         */
        private String stepType;

        /**
         * 步骤内容
         */
        private String content;

        /**
         * 使用的模型
         */
        private String modelId;

        /**
         * 消耗的 token
         */
        @Builder.Default
        private Integer tokenConsumption = 0;

        /**
         * 执行时长（毫秒）
         */
        @Builder.Default
        private Long durationMs = 0L;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
    }

    /**
     * ExecutionMessage 执行消息（支持流式）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMessage {

        /**
         * 消息 ID
         */
        private String messageId;

        /**
         * 角色 (USER, ASSISTANT, SYSTEM, TOOL)
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 是否为流式
         */
        @Builder.Default
        private Boolean streaming = false;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;

        /**
         * 检查是否为流式
         */
        public boolean isStreaming() {
            return Boolean.TRUE.equals(streaming);
        }
    }
}

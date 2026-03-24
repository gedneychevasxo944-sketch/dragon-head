package org.dragon.observer.log;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModificationLog 修改日志实体
 * 记录所有对 Organization Personality 和 Character Mind 的修改操作
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModificationLog {

    /**
     * 目标类型
     */
    public enum TargetType {
        CHARACTER,
        WORKSPACE,
        MEMORY,
        SKILL
    }

    /**
     * 触发源
     */
    public enum TriggerSource {
        OBSERVER,   // Observer 触发
        ADMIN,      // 管理员触发
        SYSTEM      // 系统触发
    }

    /**
     * 修改日志唯一标识
     */
    private String id;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 修改前快照 (JSON 格式)
     */
    private String beforeSnapshot;

    /**
     * 修改后快照 (JSON 格式)
     */
    private String afterSnapshot;

    /**
     * 触发源
     */
    private TriggerSource triggerSource;

    /**
     * 关联的评价记录 ID (Observer 触发时)
     */
    private String evaluationId;

    /**
     * 修改原因
     */
    private String reason;

    /**
     * 操作者
     */
    private String operator;

    /**
     * 操作时间
     */
    private LocalDateTime timestamp;

    /**
     * 扩展字段
     */
    private java.util.Map<String, Object> extensions;
}

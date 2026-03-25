package org.dragon.skill.registry;

import lombok.Builder;
import lombok.Data;
import org.dragon.skill.model.SkillEntry;

import java.time.LocalDateTime;

/**
 * 运行时 Skill 条目包装类。
 * 在内存中持有 SkillEntry（解析内容）+ 运行时状态信息。
 * 不持久化，进程重启后重新构建。
 */
@Data
@Builder
public class SkillRuntimeEntry {

    /** 解析后的完整技能条目 */
    private SkillEntry skillEntry;

    /** 当前运行时状态 */
    private SkillRuntimeState state;

    /** 加载失败时的错误信息（state=FAILED 时有值） */
    private String errorMessage;

    /** 最近一次状态变更时间 */
    private LocalDateTime stateChangedAt;

    /** 所属工作空间 ID（冗余存储，方便按 workspace 快速过滤） */
    private Long workspaceId;
}

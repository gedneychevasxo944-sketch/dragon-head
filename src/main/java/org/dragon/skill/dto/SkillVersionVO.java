package org.dragon.skill.dto;

import org.dragon.skill.enums.SkillVersionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SkillVersionSummaryVO — 技能版本列表项 VO。
 *
 * <p>用于版本列表接口，展示版本号、状态、创建信息和发版备注。
 */
@Data
@Builder
public class SkillVersionVO {

    /** 版本号 */
    private Integer version;

    /** 版本状态 */
    private SkillVersionStatus versionStatus;

    /** 编辑者用户 ID */
    private Long editorId;

    /** 编辑者用户名 */
    private String editorName;

    /** 版本创建时间 */
    private LocalDateTime createdAt;

    /** 发布时间 */
    private LocalDateTime publishedAt;

    /** 发版备注 */
    private String releaseNote;
}

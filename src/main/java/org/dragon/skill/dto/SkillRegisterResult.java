package org.dragon.skill.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 技能注册/更新的响应结果。
 */
@Data
@Builder
public class SkillRegisterResult {

    /** 技能业务 UUID，首次注册时生成，后续更新保持不变 */
    private String skillId;

    /** 本次写入的版本号（首次为 1，每次更新 +1） */
    private Integer version;

    /** 写入后的状态（始终为 draft） */
    private String status;

    /**
     * 非致命警告信息（不影响注册成功）。
     * 例如：某个 frontmatter 字段值不在推荐枚举范围内，但仍被接受。
     */
    private List<String> warnings;
}


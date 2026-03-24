package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillLifecycleState;
import org.dragon.skill.model.SkillSource;

/**
 * 查询 Skill 列表请求 DTO（分页 + 过滤）。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillQueryRequest {
    /** 按名称模糊搜索 */
    private String name;
    /** 按来源过滤 */
    private SkillSource source;
    /** 按分类过滤 */
    private SkillCategory category;
    /** 按生命周期状态过滤 */
    private SkillLifecycleState lifecycleState;
    /** 按标签过滤 */
    private String tag;
    /** 页码，从 1 开始，默认 1 */
    @Builder.Default
    private int page = 1;
    /** 每页条数，默认 20 */
    @Builder.Default
    private int size = 20;
}

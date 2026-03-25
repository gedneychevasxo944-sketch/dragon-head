package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillCreatorType;
import org.dragon.skill.enums.SkillVisibility;

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
    /** 按分类过滤 */
    private SkillCategory category;
    /** 按标签过滤 */
    private String tag;
    /** 按可见性过滤 */
    private SkillVisibility visibility;
    /** 按创建人类型过滤（OFFICIAL / PERSONAL） */
    private SkillCreatorType creatorType;
    /** 按创建人 ID 过滤 */
    private Long creatorId;
    /** 按启用状态过滤（null=不过滤，true=仅启用，false=仅禁用） */
    private Boolean enabled;
    /** 页码，从 1 开始，默认 1 */
    @Builder.Default
    private int page = 1;
    /** 每页条数，默认 20 */
    @Builder.Default
    private int size = 20;
}
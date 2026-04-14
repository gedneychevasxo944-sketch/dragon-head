package org.dragon.workspace.skill;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;

/**
 * WorkspaceSkillResponse — Workspace 下 Skill 列表项响应对象。
 *
 * <p>包含 Skill 元信息及该 Skill 在此 Workspace 下的启用状态。
 *
 * @author ypf
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceSkill {

    private String skillId;
    private String name;
    private String displayName;
    private String introduction;
    private SkillCategory category;
    private SkillStatus status;
    private Integer version;
    /** 该 Skill 在此 Workspace 下是否启用 */
    private Boolean enabled;
}

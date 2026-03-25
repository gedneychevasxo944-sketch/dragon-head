package org.dragon.skill.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.model.SkillSource;
import org.dragon.skill.registry.SkillRuntimeState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Skill 响应 DTO（管理视图）。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponse {
    private Long id;
    private String name;
    private SkillSource source;
    private SkillCategory category;
    private Integer version;
    private List<String> tags;
    private String description;
    private String skillDir;
    private Boolean enabled;
    private Long workspaceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 运行时状态（来自 SkillRegistry，非数据库字段）。
     * 查询列表时按需填充，可能为 null（若未加载）。
     */
    private SkillRuntimeState runtimeState;

    /**
     * 运行时错误信息（来自 SkillRegistry，非数据库字段）。
     * 仅 runtimeState=FAILED 时有值。
     */
    private String runtimeError;
}

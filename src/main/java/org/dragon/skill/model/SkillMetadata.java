package org.dragon.skill.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 解析后的 DragonHead 专属技能元数据。
 * 包含技能的条件激活和运行环境配置。
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillMetadata {
    /** 是否始终激活（忽略依赖检查） */
    Boolean always;
    /** 支持的操作系统列表 */
    List<String> os;
    /** 技能依赖要求 */
    SkillRequires requires;
    /** 技能安装规范列表 */
    List<SkillInstallSpec> install;
    /** 允许使用的工具列表（用于 Skill 级工具过滤） */
    List<String> allowedTools;
}
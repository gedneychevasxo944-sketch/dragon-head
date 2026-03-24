package org.dragon.skill.model;

import lombok.Value;

import java.util.List;

/**
 * 解析后的 DragonHead 专属技能元数据。
 * 包含技能的条件激活和运行环境配置。
 *
 * @since 1.0
 */
@Value
public class SkillMetadata {
    /** 是否始终激活（忽略依赖检查） */
    Boolean always;
    /** 技能唯一键 */
    String skillKey;
    /** 主环境变量名 */
    String primaryEnv;
    /** 技能图标（emoji） */
    String emoji;
    /** 技能主页链接 */
    String homepage;
    /** 支持的操作系统列表 */
    List<String> os;
    /** 技能依赖要求 */
    SkillRequires requires;
    /** 技能安装规范列表 */
    List<SkillInstallSpec> install;
}

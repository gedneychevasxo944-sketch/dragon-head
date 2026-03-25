package org.dragon.skill.model;

import lombok.Value;

import java.util.List;

/**
 * 技能的依赖要求。
 * 定义技能正常运行所需的环境条件。
 *
 * @since 1.0
 */
@Value
public class SkillRequires {
    /** 必须存在的可执行文件列表 */
    List<String> bins;
    /** 任一存在的可执行文件列表 */
    List<String> anyBins;
    /** 必须存在的环境变量列表 */
    List<String> env;
    /** 必须为真的配置路径列表 */
    List<String> config;
}

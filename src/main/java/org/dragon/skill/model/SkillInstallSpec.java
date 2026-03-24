package org.dragon.skill.model;

import lombok.Value;

import java.util.List;

/**
 * 技能依赖的安装规范。
 * 定义技能所需依赖的安装方式和配置。
 *
 * @since 1.0
 */
@Value
public class SkillInstallSpec {
    /** 安装类型：brew | node | go | uv | download */
    String kind;
    /** 依赖标识符 */
    String id;
    /** 显示标签 */
    String label;
    /** 可执行文件列表 */
    List<String> bins;
    /** 支持的操作系统列表 */
    List<String> os;
    /** brew 公式 */
    String formula;
    /** node 包名 */
    String pkg;
    /** go 模块 */
    String module;
    /** 下载链接 */
    String url;
    /** 压缩包名称 */
    String archive;
    /** 是否解压 */
    Boolean extract;
    /** 路径剥离层级数 */
    Integer stripComponents;
    /** 目标目录 */
    String targetDir;
}

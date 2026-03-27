package org.dragon.character.config;

import java.util.List;

/**
 * Built-in Character 配置定义
 * 统一描述所有 built-in character 的元数据
 *
 * @author wyj
 * @version 1.0
 */
public record BuiltInCharacterDefinition(
    /** Character 类型，如 "hr", "project_manager" */
    String type,
    /** Character ID 前缀，如 "hr_" */
    String idPrefix,
    /** Character 名称 */
    String name,
    /** Character 描述 */
    String description,
    /** 允许使用的工具名称列表（从 ToolRegistry 查找） */
    List<String> toolNames,
    /** 该 character 使用的 prompt key 列表 */
    List<String> promptKeys
) {}

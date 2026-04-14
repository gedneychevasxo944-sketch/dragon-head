package org.dragon.tool.store;

import org.dragon.tool.domain.ToolDO;

import java.util.List;
import java.util.Optional;

/**
 * ToolStore — tool 表的存储抽象接口。
 *
 * <p>设计说明：
 * <ul>
 *   <li>tool 表存储工具元信息，版本内容在 tool_version 表</li>
 *   <li>publishedVersionId 指向当前已发布的版本</li>
 *   <li>状态流转：draft → active → disabled（软删除用 deleted_at）</li>
 * </ul>
 *
 * <p>对齐 {@code SkillStore} 的设计风格。
 */
public interface ToolStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 保存一个新的工具记录（INSERT）。
     *
     * @param tool 工具主记录
     */
    void save(ToolDO tool);

    /**
     * 更新工具元信息（UPDATE by id）。
     *
     * @param tool 工具主记录（id 不可为 null）
     */
    void update(ToolDO tool);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 按 toolId 查询工具。
     *
     * @param toolId 工具 ID
     * @return Optional 包装的工具主记录
     */
    Optional<ToolDO> findById(String toolId);

    /**
     * 批量按 toolId 查询工具（用于绑定关系的批量加载）。
     *
     * @param toolIds 工具 ID 列表
     * @return 工具列表（仅包含存在且未软删除的记录）
     */
    List<ToolDO> findByIds(List<String> toolIds);

    /**
     * 查询所有已发布的内置工具（builtin=true, status=ACTIVE, publishedVersionId 不为空）。
     *
     * <p>用于 {@code ToolRegistry} 懒加载时获取内置工具集合。
     *
     * @return 内置工具列表，按 name 升序
     */
    List<ToolDO> findAllBuiltin();

    /**
     * 判断 toolId 是否存在（含软删除记录）。
     *
     * @param toolId 工具 ID
     * @return true 表示存在
     */
    boolean existsById(String toolId);

    /**
     * 按工具名精确查询（未软删除的记录）。
     *
     * @param name 工具名称
     * @return Optional 包装的工具主记录
     */
    Optional<ToolDO> findByName(String name);

    /**
     * 按工具名前缀批量将工具状态设为 DISABLED（MCP Server 禁用/删除时调用）。
     *
     * <p>实现层使用 {@code name LIKE 'prefix%'} 匹配，仅更新 status=ACTIVE 的工具。
     * 示例前缀：{@code mcp__github__}。
     *
     * @param namePrefix 工具名前缀（如 {@code mcp__github__}）
     */
    void disableByNamePrefix(String namePrefix);

    /**
     * 查询所有未软删除的工具（管理页面全量列表使用）。
     *
     * <p>不含状态过滤，包含 DRAFT / ACTIVE / DISABLED 等所有状态，
     * 但排除 deletedAt 不为 null 的软删除记录。
     *
     * @return 所有有效工具列表，按 createdAt 降序
     */
    List<ToolDO> findAll();
}


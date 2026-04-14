package org.dragon.tool.mcp;

import java.util.List;
import java.util.Optional;

/**
 * McpServerStore — mcp_servers 表的存储抽象接口。
 *
 * <p>设计说明：
 * <ul>
 *   <li>仅定义存储操作抽象，不包含业务逻辑（name 不可修改等校验由 {@link McpServerService} 负责）</li>
 *   <li>软删除通过 {@link McpServerDO#getDeletedAt()} 标记，{@code findAll} 等默认不返回已删除记录</li>
 *   <li>对齐 {@code ToolStore} / {@code SkillStore} 的设计风格</li>
 * </ul>
 */
public interface McpServerStore {

    // ── 写操作 ────────────────────────────────────────────────────────

    /**
     * 保存一个新的 MCP Server 配置（INSERT）。
     *
     * @param server MCP Server 配置记录
     */
    void save(McpServerDO server);

    /**
     * 更新 MCP Server 配置（UPDATE by id）。
     *
     * <p>注意：实现层只做字段写入，不做任何业务约束。
     * {@link McpServerDO#getName()} 的不可修改校验由 {@link McpServerService} 在调用前完成。
     *
     * @param server MCP Server 配置记录（id 不可为 null）
     */
    void update(McpServerDO server);

    // ── 读操作 ────────────────────────────────────────────────────────

    /**
     * 按自增主键查询（含已软删除记录）。
     *
     * @param id 主键
     * @return Optional 包装的 MCP Server 配置
     */
    Optional<McpServerDO> findById(Long id);

    /**
     * 按 name 查询（唯一索引，含已软删除记录）。
     *
     * <p>用途：
     * <ol>
     *   <li>创建时检查 name 是否已存在（需包含已软删除，防止重名）</li>
     *   <li>运行时按 serverName 定位配置</li>
     * </ol>
     *
     * @param name MCP Server 唯一标识名
     * @return Optional 包装的 MCP Server 配置
     */
    Optional<McpServerDO> findByName(String name);

    /**
     * 查询所有未删除的 MCP Server 配置，按创建时间升序。
     *
     * <p>应用启动时由 {@link McpServerService} 调用，全量加载到内存。
     *
     * @return 所有未删除的 MCP Server 列表
     */
    List<McpServerDO> findAll();

    /**
     * 查询所有未删除且 enabled=true 的 MCP Server 配置，按创建时间升序。
     *
     * <p>用于 McpToolLoader 只加载启用状态的 server。
     *
     * @return 启用中的 MCP Server 列表
     */
    List<McpServerDO> findAllEnabled();

    /**
     * 判断指定 name 的 MCP Server 是否存在（含已软删除记录）。
     *
     * <p>用于 name 唯一性校验（防止创建重名 server）。
     *
     * @param name MCP Server 唯一标识名
     * @return true 表示存在（包含已软删除）
     */
    boolean existsByName(String name);
}


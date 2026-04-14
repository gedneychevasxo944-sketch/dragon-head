package org.dragon.tool.store;

import org.dragon.tool.domain.ToolVersionDO;
import org.dragon.tool.enums.ToolVersionStatus;

import java.util.List;
import java.util.Optional;

/**
 * ToolVersionStore — tool_version 表的存储抽象接口。
 *
 * <p>对齐 {@code SkillVersionStore} 的设计风格。
 */
public interface ToolVersionStore {

    /**
     * 保存一个新版本记录。
     */
    void save(ToolVersionDO version);

    /**
     * 更新版本记录。
     */
    void update(ToolVersionDO version);

    /**
     * 按物理主键查询。
     */
    Optional<ToolVersionDO> findById(Long id);

    /**
     * 查询指定 toolId + version。
     */
    Optional<ToolVersionDO> findByToolIdAndVersion(String toolId, int version);

    /**
     * 查询指定 toolId 的所有版本，按 version 升序。
     */
    List<ToolVersionDO> findAllByToolId(String toolId);

    /**
     * 查询指定 toolId 的最新版本。
     */
    Optional<ToolVersionDO> findLatestByToolId(String toolId);

    /**
     * 查询指定 toolId 的当前 DRAFT 版本。
     */
    Optional<ToolVersionDO> findDraftByToolId(String toolId);

    /**
     * 查询指定 toolId 的已发布版本（status = PUBLISHED）。
     */
    Optional<ToolVersionDO> findPublishedByToolId(String toolId);

    /**
     * 查询最大版本号。
     */
    int findMaxVersionByToolId(String toolId);

    /**
     * 查询指定 toolId 的指定状态版本列表。
     */
    List<ToolVersionDO> findByToolIdAndStatus(String toolId, ToolVersionStatus status);
}


package org.dragon.skill.store;

import org.dragon.skill.domain.SkillVersionDO;

import java.util.List;
import java.util.Optional;

/**
 * SkillVersionStore — skill_versions 表的存储抽象接口。
 */
public interface SkillVersionStore {

    /**
     * 保存一个新版本记录。
     */
    void save(SkillVersionDO version);

    /**
     * 更新版本记录。
     */
    void update(SkillVersionDO version);

    /**
     * 按物理主键查询。
     */
    Optional<SkillVersionDO> findById(Long id);

    /**
     * 查询指定 skillId + version。
     */
    Optional<SkillVersionDO> findBySkillIdAndVersion(String skillId, int version);

    /**
     * 查询指定 skillId 的所有版本，按 version 升序。
     */
    List<SkillVersionDO> findAllBySkillId(String skillId);

    /**
     * 查询指定 skillId 的最新版本。
     */
    Optional<SkillVersionDO> findLatestBySkillId(String skillId);

    /**
     * 查询指定 skillId 的最新 DRAFT 版本。
     */
    Optional<SkillVersionDO> findDraftBySkillId(String skillId);

    /**
     * 查询最大版本号。
     */
    int findMaxVersionBySkillId(String skillId);
}

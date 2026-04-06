package org.dragon.skill.store;

import org.dragon.skill.actionlog.SkillActionLog;
import org.dragon.skill.enums.SkillActionType;
import org.dragon.store.Store;

import java.util.List;

/**
 * Skill 操作日志存储接口。
 */
public interface SkillActionLogStore extends Store {

    /**
     * 保存日志。
     *
     * @param log 日志
     */
    void save(SkillActionLog log);

    /**
     * 分页查询某 Skill 的操作日志。
     *
     * @param skillId    技能 UUID
     * @param offset     偏移量
     * @param limit      每页条数
     * @return 日志列表，按时间降序
     */
    List<SkillActionLog> findBySkillId(String skillId, int offset, int limit);

    /**
     * 统计某 Skill 的操作日志总数。
     *
     * @param skillId 技能 UUID
     * @return 日志总数
     */
    int countBySkillId(String skillId);

    /**
     * 按操作类型查询某 Skill 的操作日志。
     *
     * @param skillId    技能 UUID
     * @param actionType 操作类型
     * @param offset     偏移量
     * @param limit      每页条数
     * @return 日志列表
     */
    List<SkillActionLog> findBySkillIdAndActionType(String skillId, SkillActionType actionType,
                                                     int offset, int limit);
}

package org.dragon.skill.actionlog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.dto.PageResult;
import org.dragon.skill.enums.SkillActionType;
import org.dragon.skill.store.SkillActionLogStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Skill 操作日志服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillActionLogService {

    private final SkillActionLogStore actionLogStore;

    // ── 记录日志 ──────────────────────────────────────────────────────

    /**
     * 记录操作日志。
     *
     * @param skillId      技能 UUID
     * @param skillName    技能名称
     * @param actionType   操作类型
     * @param operatorId   操作人 ID
     * @param operatorName 操作人名称
     * @param version      版本号
     * @param detail       操作详情（可为 null）
     */
    public void log(String skillId, String skillName, SkillActionType actionType,
                    Long operatorId, String operatorName, Integer version,
                    ActionDetail detail) {
        SkillActionLog actionLog = SkillActionLog.builder()
                .id(UUID.randomUUID().toString())
                .skillId(skillId)
                .skillName(skillName)
                .actionType(actionType)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .version(version)
                .detail(detail)
                .build();

        actionLogStore.save(actionLog);
        log.debug("[SkillActionLogService] Logged action: skillId={}, action={}, operator={}",
                skillId, actionType, operatorName);
    }

    /**
     * 记录操作日志（无详情）。
     */
    public void log(String skillId, String skillName, SkillActionType actionType,
                    Long operatorId, String operatorName, Integer version) {
        log(skillId, skillName, actionType, operatorId, operatorName, version, null);
    }

    // ── 查询日志 ──────────────────────────────────────────────────────

    /**
     * 分页查询某 Skill 的操作日志。
     *
     * @param skillId 技能 UUID
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @return 分页结果
     */
    public PageResult<SkillActionLogVO> pageBySkill(String skillId, int page, int size) {
        int offset = (page - 1) * size;
        List<SkillActionLog> logs = actionLogStore.findBySkillId(skillId, offset, size);
        int total = actionLogStore.countBySkillId(skillId);

        List<SkillActionLogVO> items = logs.stream()
                .map(SkillActionLogVO::fromDomain)
                .toList();

        return PageResult.of(items, total, page, size);
    }

    /**
     * 按操作类型分页查询某 Skill 的操作日志。
     *
     * @param skillId    技能 UUID
     * @param actionType 操作类型
     * @param page       页码
     * @param size       每页条数
     * @return 分页结果
     */
    public PageResult<SkillActionLogVO> pageBySkillAndActionType(String skillId,
                                                                  SkillActionType actionType,
                                                                  int page, int size) {
        int offset = (page - 1) * size;
        List<SkillActionLog> logs = actionLogStore.findBySkillIdAndActionType(skillId, actionType, offset, size);
        int total = actionLogStore.countBySkillId(skillId);

        List<SkillActionLogVO> items = logs.stream()
                .map(SkillActionLogVO::fromDomain)
                .toList();

        return PageResult.of(items, total, page, size);
    }
}

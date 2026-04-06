package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.skill.actionlog.SkillActionLogService;
import org.dragon.skill.actionlog.SkillActionLogVO;
import org.dragon.skill.dto.SkillDetailVO;
import org.dragon.skill.dto.SkillRegisterRequest;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillRegisterResult;
import org.dragon.skill.dto.SkillSummaryVO;
import org.dragon.skill.dto.SkillVersionSummaryVO;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillStatus;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.service.SkillLifecycleService;
import org.dragon.skill.service.SkillQueryService;
import org.dragon.skill.service.SkillRegisterService;
import org.dragon.util.UserUtils;
import org.dragon.util.bean.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * SkillApplication Skill 模块应用服务
 *
 * <p>对应前端 /skills 页面，聚合技能的 CRUD、版本发布、草稿保存等业务逻辑。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillApplication {

    private final SkillRegisterService registerService;
    private final SkillLifecycleService lifecycleService;
    private final SkillQueryService queryService;
    private final SkillActionLogService actionLogService;

    // ==================== Skill CRUD ====================

    /**
     * 创建技能。
     */
    public SkillDetailVO create(MultipartFile file, SkillRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        SkillRegisterResult result = registerService.register(file, request, user);
        log.info("[SkillApplication] Created skill: skillId={}, version={}",
                result.getSkillId(), result.getVersion());
        return queryService.getDetail(result.getSkillId(), false);
    }

    /**
     * 更新技能。
     */
    public SkillDetailVO update(String skillId, MultipartFile file, SkillRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        SkillRegisterResult result = registerService.update(skillId, file, request, user);
        log.info("[SkillApplication] Updated skill: skillId={}, version={}",
                result.getSkillId(), result.getVersion());
        return queryService.getDetail(result.getSkillId(), false);
    }

    /**
     * 分页获取技能列表。
     */
    public PageResponse<SkillSummaryVO> listSkills(int page, int pageSize, String search,
                                                  String visibility, String status,
                                                  String category) {
        SkillQueryRequest request = new SkillQueryRequest();
        request.setKeyword(search);
        // 转换 String 到枚举类型
        if (category != null && !category.isBlank()) {
            try {
                request.setCategory(SkillCategory.valueOf(category.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // 忽略无效的 category 值
            }
        }
        if (visibility != null && !visibility.isBlank()) {
            try {
                request.setVisibility(SkillVisibility.valueOf(visibility.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // 忽略无效的 visibility 值
            }
        }
        if (status != null && !status.isBlank()) {
            try {
                request.setStatus(SkillStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // 忽略无效的状态值
            }
        }

        var result = queryService.pageSearch(request);
        return PageResponse.of(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
    }

    /**
     * 获取技能详情。
     */
    public SkillDetailVO getSkill(String skillId) {
        return queryService.getDetail(skillId, true);
    }

    /**
     * 删除技能。
     */
    public void deleteSkill(String skillId) {
        UserInfo user = UserUtils.getUserInfo();
        lifecycleService.delete(skillId, user);
        log.info("[SkillApplication] Deleted skill: {}", skillId);
    }

    /**
     * 发布技能版本。
     */
    public SkillDetailVO publishSkill(String skillId, String version, String changelog) {
        UserInfo user = UserUtils.getUserInfo();
        int ver = version != null ? Integer.parseInt(version) : 1;
        lifecycleService.publish(skillId, ver, changelog, user);
        log.info("[SkillApplication] Published skill: {} version: {}", skillId, version);
        return queryService.getDetail(skillId, false);
    }

    /**
     * 禁用技能。
     */
    public void disableSkill(String skillId) {
        UserInfo user = UserUtils.getUserInfo();
        lifecycleService.disable(skillId, user);
    }

    /**
     * 启用技能。
     */
    public void enableSkill(String skillId) {
        UserInfo user = UserUtils.getUserInfo();
        lifecycleService.republish(skillId, user);
    }

    /**
     * 获取技能版本列表。
     */
    public List<SkillVersionSummaryVO> listVersions(String skillId) {
        return queryService.listVersions(skillId);
    }

    /**
     * 保存技能草稿。
     * 更新最新版本的内容和元数据，保持 DRAFT 状态。
     *
     * @param skillId 技能 UUID
     * @param request 草稿内容
     * @return 注册结果
     */
    public SkillRegisterResult saveDraft(String skillId, SkillRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        SkillRegisterResult result = registerService.saveDraft(skillId, request, user);
        log.info("[SkillApplication] Saved draft for skill: {}", skillId);
        return result;
    }

    /**
     * 获取技能活动日志（分页）。
     *
     * @param skillId 技能 UUID
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @return 分页结果
     */
    public PageResponse<SkillActionLogVO> getActivityLogs(String skillId, int page, int size) {
        var result = actionLogService.pageBySkill(skillId, page, size);
        return PageResponse.of(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
    }
}

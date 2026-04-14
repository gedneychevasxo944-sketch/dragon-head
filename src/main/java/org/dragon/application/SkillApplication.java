package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.skill.service.SkillActionLogService;
import org.dragon.skill.dto.SkillActionLog;
import org.dragon.asset.service.AssetPublishStatusService;
import org.dragon.asset.service.CollaboratorService;
import org.dragon.skill.dto.SkillVO;
import org.dragon.skill.dto.SkillRegisterRequest;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillRegisterResult;
import org.dragon.skill.dto.SkillVersionVO;
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
    private final CollaboratorService collaboratorService;
    private final AssetPublishStatusService publishStatusService;

    // ==================== Skill CRUD ====================

    /**
     * 创建技能。
     */
    public SkillVO create(MultipartFile file, SkillRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        SkillRegisterResult result = registerService.register(file, request, user);
        log.info("[SkillApplication] Created skill: skillId={}, version={}",
                result.getSkillId(), result.getVersion());
        return queryService.getDetail(result.getSkillId());
    }

    /**
     * 更新技能。
     */
    public SkillVO update(String skillId, MultipartFile file, SkillRegisterRequest request) {
        UserInfo user = UserUtils.getUserInfo();
        SkillRegisterResult result = registerService.update(skillId, file, request, user);
        log.info("[SkillApplication] Updated skill: skillId={}, version={}",
                result.getSkillId(), result.getVersion());
        return queryService.getDetail(result.getSkillId());
    }

    /**
     * 分页获取技能列表。
     */
    public PageResponse<SkillVO> listSkills(int page, int pageSize, String search,
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
        request.setPage(page);
        request.setPageSize(pageSize);

        var result = queryService.pageSearch(request);
        return PageResponse.of(result.getList(), result.getTotal(), result.getPage(), result.getPageSize());
    }

    /**
     * 获取技能详情。
     */
    public SkillVO getSkill(String skillId) {
        return queryService.getDetail(skillId);
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
    public SkillVO publishSkill(String skillId, String version, String changelog) {
        UserInfo user = UserUtils.getUserInfo();
        int ver = version != null ? Integer.parseInt(version) : 1;
        lifecycleService.publish(skillId, ver, changelog, user);
        log.info("[SkillApplication] Published skill: {} version: {}", skillId, version);
        return queryService.getDetail(skillId);
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
    public List<SkillVersionVO> listVersions(String skillId) {
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
     * 获取技能操作日志（分页）。
     *
     * @param skillId 技能 UUID
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @return 分页结果
     */
    public PageResponse<SkillActionLog> getActionLogs(String skillId, int page, int size) {
        var result = actionLogService.pageBySkill(skillId, page, size);
        return PageResponse.of(result.getList(), result.getTotal(), result.getPage(), result.getPageSize());
    }
}

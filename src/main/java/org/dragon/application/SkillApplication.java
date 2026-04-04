package org.dragon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.api.dto.PageResponse;
import org.dragon.permission.enums.ResourceType;
import org.dragon.permission.service.PermissionService;
import org.dragon.skill.dto.SkillCreateRequest;
import org.dragon.skill.dto.SkillQueryRequest;
import org.dragon.skill.dto.SkillResponse;
import org.dragon.skill.dto.SkillUpdateRequest;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillVisibility;
import org.dragon.skill.service.SkillManageService;
import org.dragon.util.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final SkillManageService skillManageService;
    private final PermissionService permissionService;

    // ==================== Skill CRUD ====================

    /**
     * 分页获取技能列表。
     *
     * @param page          页码
     * @param pageSize      每页数量
     * @param search        搜索关键词
     * @param visibility    可见性筛选
     * @param assetState    资产状态筛选
     * @param runtimeStatus 运行时状态筛选
     * @param category      分类筛选
     * @return 分页结果
     */
    public PageResponse<SkillResponse> listSkills(int page, int pageSize, String search,
                                                  String visibility, String assetState,
                                                  String runtimeStatus, String category) {
        SkillQueryRequest request = new SkillQueryRequest();
        request.setName(search);
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

        List<SkillResponse> all = skillManageService.listSkills(request);

        // 按用户可见性过滤
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<String> visibleIds = permissionService.getVisibleAssets(ResourceType.SKILL, userId);

        List<SkillResponse> filtered = all.stream()
                .filter(skill -> {
                    if (visibleIds != null && !visibleIds.isEmpty()) {
                        String skillId = String.valueOf(skill.getId());
                        if (!visibleIds.contains(skillId)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<SkillResponse> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);
        return PageResponse.of(pageData, total, page, pageSize);
    }

    /**
     * 创建技能。
     *
     * @param file    技能 ZIP 包
     * @param request 技能创建请求
     * @return 创建后的技能
     */
    public SkillResponse createSkill(MultipartFile file, SkillCreateRequest request) {
        SkillResponse response = skillManageService.createSkill(file, request);
        log.info("[SkillApplication] Created skill: {}", response.getId());
        return response;
    }

    /**
     * 获取技能详情。
     *
     * @param skillId 技能 ID
     * @return 技能
     */
    public SkillResponse getSkill(Long skillId) {
        return skillManageService.getSkill(skillId);
    }

    /**
     * 更新技能。
     *
     * @param skillId 技能 ID
     * @param file    新的 ZIP 包（可选）
     * @param request 更新请求
     * @return 更新后的技能
     */
    public SkillResponse updateSkill(Long skillId, MultipartFile file, SkillUpdateRequest request) {
        SkillResponse response = skillManageService.updateSkill(skillId, file, request);
        log.info("[SkillApplication] Updated skill: {}", skillId);
        return response;
    }

    /**
     * 删除技能。
     *
     * @param skillId 技能 ID
     */
    public void deleteSkill(Long skillId) {
        skillManageService.deleteSkill(skillId);
        log.info("[SkillApplication] Deleted skill: {}", skillId);
    }

    /**
     * 发布技能版本。
     *
     * @param skillId   技能 ID
     * @param version   版本号
     * @param changelog 变更日志
     * @return 发布后的技能
     */
    public SkillResponse publishSkill(Long skillId, String version, String changelog) {
        // 目前 SkillManageService 中没有单独的 publish 接口，通过 enable 模拟发布状态
        skillManageService.enableSkill(skillId);
        log.info("[SkillApplication] Published skill: {} version: {}", skillId, version);
        return skillManageService.getSkill(skillId);
    }

    /**
     * 禁用技能。
     *
     * @param skillId 技能 ID
     */
    public void disableSkill(Long skillId) {
        skillManageService.disableSkill(skillId);
    }

    /**
     * 启用技能。
     *
     * @param skillId 技能 ID
     */
    public void enableSkill(Long skillId) {
        skillManageService.enableSkill(skillId);
    }

    /**
     * 保存技能草稿（占位）。
     *
     * @param skillId 技能 ID
     * @param content 草稿内容
     * @return 保存后的技能
     */
    public SkillResponse saveDraft(Long skillId, Map<String, Object> content) {
        log.info("[SkillApplication] Save draft for skill: {}", skillId);
        return skillManageService.getSkill(skillId);
    }
}

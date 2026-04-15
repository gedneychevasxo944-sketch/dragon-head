package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.SkillApplication;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.skill.actionlog.SkillActionLogVO;
import org.dragon.skill.dto.SkillDetailVO;
import org.dragon.skill.dto.SkillRegisterRequest;
import org.dragon.skill.dto.SkillRegisterResult;
import org.dragon.permission.checker.PermissionChecker;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * SkillController Skill 技能模块 API
 *
 * <p>对应前端 /skills 页面，包含技能 CRUD、版本发布、草稿保存等接口。
 * Base URL: /api/v1/skills
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Skill", description = "技能模块")
@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillApplication skillApplication;
    private final PermissionChecker permissionChecker;

    // ==================== Skill CRUD ====================

    /**
     * 创建技能
     * POST /api/v1/skills (multipart/form-data)
     */
    @Operation(summary = "创建技能（ZIP 包）")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SkillDetailVO> createSkill(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("data") SkillRegisterRequest request) {
        SkillDetailVO response = skillApplication.create(file, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取技能详情
     * GET /api/v1/skills/{skillId}
     */
    @Operation(summary = "获取技能详情")
    @GetMapping("/{skillId}")
    public ApiResponse<SkillDetailVO> getSkill(@PathVariable String skillId) {
        permissionChecker.checkView("SKILL", skillId);
        SkillDetailVO response = skillApplication.getSkill(skillId);
        return ApiResponse.success(response);
    }

    /**
     * 更新技能
     * PUT /api/v1/skills/{skillId} (multipart/form-data)
     */
    @Operation(summary = "更新技能")
    @PutMapping(value = "/{skillId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SkillDetailVO> updateSkill(
            @PathVariable String skillId,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("data") SkillRegisterRequest request) {
        permissionChecker.checkEdit("SKILL", skillId);
        SkillDetailVO response = skillApplication.update(skillId, file, request);
        return ApiResponse.success(response);
    }

    /**
     * 发布技能版本
     * POST /api/v1/skills/{skillId}/publish
     */
    @Operation(summary = "发布技能版本")
    @PostMapping("/{skillId}/publish")
    public ApiResponse<SkillDetailVO> publishSkill(
            @PathVariable String skillId,
            @RequestBody PublishSkillRequest request) {
        permissionChecker.checkPermission("SKILL", skillId, "PUBLISH");
        SkillDetailVO response = skillApplication.publishSkill(skillId, request.getVersion(), request.getChangelog());
        return ApiResponse.success(response);
    }

    /**
     * 删除技能
     * DELETE /api/v1/skills/{skillId}
     */
    @Operation(summary = "删除技能")
    @DeleteMapping("/{skillId}")
    public ApiResponse<Map<String, Object>> deleteSkill(@PathVariable String skillId) {
        permissionChecker.checkDelete("SKILL", skillId);
        skillApplication.deleteSkill(skillId);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 禁用技能
     * POST /api/v1/skills/{skillId}/disable
     */
    @Operation(summary = "禁用技能")
    @PostMapping("/{skillId}/disable")
    public ApiResponse<Void> disableSkill(@PathVariable String skillId) {
        skillApplication.disableSkill(skillId);
        return ApiResponse.success(null);
    }

    /**
     * 启用技能
     * POST /api/v1/skills/{skillId}/enable
     */
    @Operation(summary = "启用技能")
    @PostMapping("/{skillId}/enable")
    public ApiResponse<Void> enableSkill(@PathVariable String skillId) {
        skillApplication.enableSkill(skillId);
        return ApiResponse.success(null);
    }

    /**
     * 获取技能版本列表
     * GET /api/v1/skills/{skillId}/versions
     */
    @Operation(summary = "获取技能版本列表")
    @GetMapping("/{skillId}/versions")
    public ApiResponse<List<SkillDetailVO>> listVersions(@PathVariable String skillId) {
        List<SkillDetailVO> response = skillApplication.listVersions(skillId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "保存技能草稿")
    @PutMapping("/{skillId}/draft")
    public ApiResponse<SkillRegisterResult> saveDraft(
            @PathVariable String skillId,
            @RequestBody SkillRegisterRequest request) {
        permissionChecker.checkEdit("SKILL", skillId);
        SkillRegisterResult response = skillApplication.saveDraft(skillId, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取技能活动日志
     * GET /api/v1/skills/{skillId}/activity-logs
     */
    @Operation(summary = "获取技能活动日志")
    @GetMapping("/{skillId}/activity-logs")
    public ApiResponse<PageResponse<SkillActionLogVO>> getActivityLogs(
            @PathVariable String skillId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResponse<SkillActionLogVO> response = skillApplication.getActivityLogs(skillId, page, pageSize);
        return ApiResponse.success(response);
    }

    // ==================== 请求体 DTO ====================

    /** 发布技能版本请求 */
    @Data
    public static class PublishSkillRequest {
        /** 版本号 */
        private String version;
        /** 变更日志 */
        private String changelog;
    }
}

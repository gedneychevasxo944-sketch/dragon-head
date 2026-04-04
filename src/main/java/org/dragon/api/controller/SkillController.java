package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.SkillApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.skill.dto.SkillCreateRequest;
import org.dragon.skill.dto.SkillResponse;
import org.dragon.skill.dto.SkillUpdateRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("isAuthenticated()")
public class SkillController {

    private final SkillApplication skillApplication;

    // ==================== 14. Skill CRUD ====================

    /**
     * 14.1 获取技能列表
     * GET /api/v1/skills
     */
    @Operation(summary = "获取技能列表（分页+筛选）")
    @GetMapping
    public ApiResponse<PageResponse<SkillResponse>> listSkills(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String assetState,
            @RequestParam(required = false) String runtimeStatus,
            @RequestParam(required = false) String category) {
        PageResponse<SkillResponse> result = skillApplication.listSkills(
                page, pageSize, search, visibility, assetState, runtimeStatus, category);
        return ApiResponse.success(result);
    }

    /**
     * 14.2 创建技能
     * POST /api/v1/skills (multipart/form-data)
     */
    @Operation(summary = "创建技能（上传 ZIP 包）")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SkillResponse> createSkill(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") SkillCreateRequest request) {
        SkillResponse response = skillApplication.createSkill(file, request);
        return ApiResponse.success(response);
    }

    /**
     * 14.3 获取技能详情
     * GET /api/v1/skills/:id
     */
    @Operation(summary = "获取技能详情")
    @GetMapping("/{id}")
    public ApiResponse<SkillResponse> getSkill(@PathVariable Long id) {
        SkillResponse response = skillApplication.getSkill(id);
        return ApiResponse.success(response);
    }

    /**
     * 14.4 更新技能
     * PUT /api/v1/skills/:id (multipart/form-data)
     */
    @Operation(summary = "更新技能")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SkillResponse> updateSkill(
            @PathVariable Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("data") SkillUpdateRequest request) {
        SkillResponse response = skillApplication.updateSkill(id, file, request);
        return ApiResponse.success(response);
    }

    /**
     * 14.5 发布技能版本
     * POST /api/v1/skills/:id/publish
     */
    @Operation(summary = "发布技能版本")
    @PostMapping("/{id}/publish")
    public ApiResponse<SkillResponse> publishSkill(
            @PathVariable Long id,
            @RequestBody PublishSkillRequest request) {
        SkillResponse response = skillApplication.publishSkill(id, request.getVersion(), request.getChangelog());
        return ApiResponse.success(response);
    }

    /**
     * 14.6 删除技能
     * DELETE /api/v1/skills/:id
     */
    @Operation(summary = "删除技能")
    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteSkill(@PathVariable Long id) {
        skillApplication.deleteSkill(id);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 14.7 保存技能草稿
     * PUT /api/v1/skills/:id/draft
     */
    @Operation(summary = "保存技能草稿")
    @PutMapping("/{id}/draft")
    public ApiResponse<SkillResponse> saveDraft(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) body.get("content");
        if (content == null) {
            content = body;
        }
        SkillResponse response = skillApplication.saveDraft(id, content);
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
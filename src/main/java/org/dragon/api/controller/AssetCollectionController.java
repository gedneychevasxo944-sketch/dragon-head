package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.api.controller.dto.PageResponse;
import org.dragon.asset.service.AssetCollectionService;
import org.dragon.asset.service.AssetMarkService;
import org.dragon.character.Character;
import org.dragon.datasource.entity.ExpertEntity;
import org.dragon.expert.service.ExpertService;
import org.dragon.observer.Observer;
import org.dragon.permission.enums.ResourceType;
import org.dragon.skill.dto.SkillSummaryVO;
import org.dragon.workspace.Workspace;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AssetCollectionController 资产统一列表 API
 *
 * <p>提供所有资产的统一列表入口，支持 Builtin/Expert 显隐控制。
 * Base URL: /api/v1/asset-collection
 *
 * @author yijunw
 */
@Tag(name = "AssetCollection", description = "资产统一列表")
@RestController
@RequestMapping("/api/v1/asset-collection")
@RequiredArgsConstructor
public class AssetCollectionController {

    private final AssetCollectionService assetCollectionService;
    private final ExpertService expertService;
    private final AssetMarkService assetMarkService;

    // ==================== Character ====================

    /**
     * 获取角色列表
     * GET /api/v1/asset-collection/characters?includeBuiltin=true&includeExpert=true
     */
    @Operation(summary = "获取角色列表（统一入口）")
    @GetMapping("/characters")
    public ApiResponse<PageResponse<Character>> listCharacters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "true") boolean includeBuiltin,
            @RequestParam(defaultValue = "true") boolean includeExpert) {
        PageResponse<Character> result = assetCollectionService.listCharacters(
                page, pageSize, search, status, source, includeBuiltin, includeExpert);
        return ApiResponse.success(result);
    }

    // ==================== Trait ====================

    /**
     * 获取 Trait 列表
     * GET /api/v1/asset-collection/traits?includeBuiltin=true&includeExpert=true
     */
    @Operation(summary = "获取 Trait 列表（统一入口）")
    @GetMapping("/traits")
    public ApiResponse<PageResponse<Map<String, Object>>> listTraits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) String publishStatus,
            @RequestParam(defaultValue = "true") boolean includeBuiltin,
            @RequestParam(defaultValue = "true") boolean includeExpert) {
        PageResponse<Map<String, Object>> result = assetCollectionService.listTraits(
                page, pageSize, search, tagName, publishStatus, includeBuiltin, includeExpert);
        return ApiResponse.success(result);
    }

    // ==================== Skill ====================

    /**
     * 获取 Skill 列表
     * GET /api/v1/asset-collection/skills
     */
    @Operation(summary = "获取 Skill 列表（统一入口）")
    @GetMapping("/skills")
    public ApiResponse<PageResponse<SkillSummaryVO>> listSkills(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String assetState,
            @RequestParam(required = false) String runtimeStatus,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "true") boolean includeBuiltin,
            @RequestParam(defaultValue = "true") boolean includeExpert) {
        PageResponse<SkillSummaryVO> result = assetCollectionService.listSkills(
                page, pageSize, search, visibility, assetState, runtimeStatus, category,
                includeBuiltin, includeExpert);
        return ApiResponse.success(result);
    }

    // ==================== Workspace ====================

    /**
     * 获取 Workspace 列表
     * GET /api/v1/asset-collection/workspaces
     */
    @Operation(summary = "获取 Workspace 列表（统一入口）")
    @GetMapping("/workspaces")
    public ApiResponse<PageResponse<Workspace>> listWorkspaces(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "true") boolean includeBuiltin,
            @RequestParam(defaultValue = "true") boolean includeExpert) {
        PageResponse<Workspace> result = assetCollectionService.listWorkspaces(
                page, pageSize, search, status, includeBuiltin, includeExpert);
        return ApiResponse.success(result);
    }

    // ==================== Observer ====================

    /**
     * 获取 Observer 列表
     * GET /api/v1/asset-collection/observers
     */
    @Operation(summary = "获取 Observer 列表（统一入口）")
    @GetMapping("/observers")
    public ApiResponse<PageResponse<Observer>> listObservers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean includeBuiltin,
            @RequestParam(defaultValue = "true") boolean includeExpert) {
        PageResponse<Observer> result = assetCollectionService.listObservers(
                page, pageSize, search, includeBuiltin, includeExpert);
        return ApiResponse.success(result);
    }

    // ==================== Expert ====================

    /**
     * 获取 Expert 列表
     * GET /api/v1/asset-collection/experts
     */
    @Operation(summary = "获取 Expert 列表")
    @GetMapping("/experts")
    public ApiResponse<List<ExpertVO>> listExperts(
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) String category) {
        List<ExpertEntity> marks = expertService.listExperts(resourceType, category);
        List<ExpertVO> vos = marks.stream()
                .map(this::toExpertVO)
                .toList();
        return ApiResponse.success(vos);
    }

    // ==================== Builtin ====================

    /**
     * 获取 Builtin 列表
     * GET /api/v1/asset-collection/builtins
     */
    @Operation(summary = "获取 Builtin 列表")
    @GetMapping("/builtins")
    public ApiResponse<List<BuiltinVO>> listBuiltins(
            @RequestParam(required = false) ResourceType resourceType) {
        List<ExpertEntity> marks = assetMarkService.listBuiltins(resourceType);
        List<BuiltinVO> vos = marks.stream()
                .map(this::toBuiltinVO)
                .toList();
        return ApiResponse.success(vos);
    }

    // ==================== DTO ====================

    @Data
    @Builder
    public static class ExpertVO {
        private String id;
        private ResourceType resourceType;
        private String resourceId;
        private String category;
        private String preview;
        private String targetAudience;
        private Integer usageCount;
        private Object asset;

        public static ExpertVO from(ExpertEntity mark) {
            return ExpertVO.builder()
                    .id(mark.getId())
                    .resourceType(mark.getResourceType())
                    .resourceId(mark.getResourceId())
                    .category(mark.getCategory())
                    .preview(mark.getPreview())
                    .targetAudience(mark.getTargetAudience())
                    .usageCount(mark.getUsageCount())
                    .build();
        }
    }

    @Data
    @Builder
    public static class BuiltinVO {
        private String id;
        private ResourceType resourceType;
        private String resourceId;
        private Object asset;

        public static BuiltinVO from(ExpertEntity mark) {
            return BuiltinVO.builder()
                    .id(mark.getId())
                    .resourceType(mark.getResourceType())
                    .resourceId(mark.getResourceId())
                    .build();
        }
    }

    // ==================== 内部方法 ====================

    private ExpertVO toExpertVO(ExpertEntity mark) {
        ExpertVO vo = ExpertVO.from(mark);
        vo.setAsset(expertService.getExpertAsset(mark.getResourceType(), mark.getResourceId()));
        return vo;
    }

    private BuiltinVO toBuiltinVO(ExpertEntity mark) {
        return BuiltinVO.from(mark);
    }
}
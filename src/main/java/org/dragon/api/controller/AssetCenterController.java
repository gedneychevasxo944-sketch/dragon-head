package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.asset.dto.AssetMemberDTO;
import org.dragon.asset.service.AssetMemberService;
import org.dragon.util.UserUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AssetCenterController 资产中心 API
 *
 * <p>对应前端 /studio/assets 页面，提供资产仪表盘和统计功能。
 * Base URL: /api/v1/assets
 *
 * @author yijunw
 * @version 1.0
 */
@Tag(name = "AssetCenter", description = "资产中心")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetCenterController {

    private final AssetMemberService assetMemberService;

    /**
     * 获取当前用户的资产仪表盘
     * GET /api/v1/assets
     */
    @Operation(summary = "获取当前用户的资产仪表盘")
    @GetMapping
    public ApiResponse<List<AssetMemberDTO>> getMyAssets() {
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<AssetMemberDTO> assets = assetMemberService.getMyAssets(userId);
        return ApiResponse.success(assets);
    }

    /**
     * 获取资产统计汇总
     * GET /api/v1/assets/summary
     */
    @Operation(summary = "获取资产统计汇总")
    @GetMapping("/summary")
    public ApiResponse<Map<String, Long>> getAssetSummary() {
        Long userId = Long.parseLong(UserUtils.getUserId());
        List<AssetMemberDTO> assets = assetMemberService.getMyAssets(userId);

        // 按资源类型统计数量
        Map<String, Long> summary = assets.stream()
                .collect(Collectors.groupingBy(
                        asset -> asset.getResourceType().name(),
                        Collectors.counting()
                ));

        return ApiResponse.success(summary);
    }
}
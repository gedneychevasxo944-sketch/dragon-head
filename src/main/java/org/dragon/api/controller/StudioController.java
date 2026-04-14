package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.dragon.api.controller.dto.ApiResponse;
import org.dragon.character.service.CharacterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * StudioController Studio 模块 API
 *
 * <p>对应前端 /studio 页面，保留 Studio 统计等核心功能。
 * Base URL: /api/v1/studio
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Studio", description = "Studio 模块")
@RestController
@RequestMapping("/api/v1/studio")
@RequiredArgsConstructor
public class StudioController {

    private final CharacterService characterService;

    /**
     * 获取 Studio 统计数据
     * GET /api/v1/studio/stats
     */
    @Operation(summary = "获取 Studio 统计数据")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStudioStats() {
        Map<String, Object> stats = characterService.getCharacterStats();
        return ApiResponse.success(stats);
    }
}
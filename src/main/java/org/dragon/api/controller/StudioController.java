package org.dragon.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.dragon.application.StudioApplication;
import org.dragon.api.dto.ApiResponse;
import org.dragon.api.dto.PageResponse;
import org.dragon.character.Character;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * StudioController Studio 模块 API
 *
 * <p>对应前端 /studio 页面，包含 Character、Trait、Template、Deployment 相关接口。
 * Base URL: /api/v1/studio
 *
 * @author zhz
 * @version 1.0
 */
@Tag(name = "Studio", description = "Studio 模块：角色、特征、模板、派驻")
@RestController
@RequestMapping("/api/v1/studio")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StudioController {

    private final StudioApplication studioApplication;

    // ==================== 1. Character（角色）====================

    /**
     * 1.1 获取角色列表
     * GET /api/v1/studio/characters
     */

    @Operation(summary = "获取角色列表（分页+筛选）")
    @GetMapping("/characters")
    public ApiResponse<PageResponse<Character>> listCharacters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        PageResponse<Character> result = studioApplication.listCharacters(page, pageSize, search, status, source);
        return ApiResponse.success(result);
    }

    /**
     * 1.2 创建角色
     * POST /api/v1/studio/characters
     */
    @Operation(summary = "创建角色")
    @PostMapping("/characters")
    public ApiResponse<Character> createCharacter(@RequestBody Character character) {
        Character created = studioApplication.createCharacter(character);
        return ApiResponse.success(created);
    }

    /**
     * 1.3 获取角色详情
     * GET /api/v1/studio/characters/:id
     */
    @Operation(summary = "获取角色详情")
    @GetMapping("/characters/{id}")
    public ApiResponse<Character> getCharacter(@PathVariable String id) {
        return studioApplication.getCharacter(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error(404, "Character not found: " + id));
    }

    /**
     * 1.4 更新角色
     * PUT /api/v1/studio/characters/:id
     */
    @Operation(summary = "更新角色")
    @PutMapping("/characters/{id}")
    public ApiResponse<Character> updateCharacter(
            @PathVariable String id,
            @RequestBody Character character) {
        Character updated = studioApplication.updateCharacter(id, character);
        return ApiResponse.success(updated);
    }

    /**
     * 1.5 删除角色
     * DELETE /api/v1/studio/characters/:id
     */
    @Operation(summary = "删除角色")
    @DeleteMapping("/characters/{id}")
    public ApiResponse<Map<String, Object>> deleteCharacter(@PathVariable String id) {
        studioApplication.deleteCharacter(id);
        return ApiResponse.success(Map.of("success", true));
    }

    /**
     * 1.6 获取 Studio 统计数据
     * GET /api/v1/studio/stats
     */
    @Operation(summary = "获取 Studio 统计数据")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStudioStats() {
        Map<String, Object> stats = studioApplication.getStudioStats();
        return ApiResponse.success(stats);
    }

    /**
     * 1.7 独立运行角色（发送消息）
     * POST /api/v1/studio/characters/:id/run
     */
    @Operation(summary = "独立运行角色 - 发送消息")
    @PostMapping("/characters/{id}/run")
    public ApiResponse<Map<String, Object>> runCharacter(
            @PathVariable String id,
            @RequestBody RunCharacterRequest request) {
        Map<String, Object> result = studioApplication.runCharacter(id, request.getMessage(), request.getSessionId());
        return ApiResponse.success(result);
    }

    // ==================== 2. Trait（特征片段）====================

    /**
     * 2.1 获取 Trait 列表
     * GET /api/v1/studio/traits
     */
    @Operation(summary = "获取 Trait 列表")
    @GetMapping("/traits")
    public ApiResponse<PageResponse<Map<String, Object>>> listTraits(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category) {
        // 临时实现：返回 mock 数据
        List<Map<String, Object>> mockTraits = List.of(
                Map.of("id", "trait_001", "name", "结构化思维", "type", "personality", "category", "行事原则", "description", "倾向于用逻辑和结构化的方式处理信息和问题"),
                Map.of("id", "trait_002", "name", "数据驱动", "type", "personality", "category", "决策风格", "description", "决策基于数据分析而非直觉"),
                Map.of("id", "trait_003", "name", "风险管理意识", "type", "personality", "category", "风险偏好", "description", "主动识别和评估潜在风险"),
                Map.of("id", "trait_004", "name", "批判性思维", "type", "personality", "category", "决策风格", "description", "不轻信信息，善于质疑和分析"),
                Map.of("id", "trait_005", "name", "高效协作", "type", "personality", "category", "协作风格", "description", "擅长与他人合作，共同完成任务"),
                Map.of("id", "trait_006", "name", "温暖同理", "type", "personality", "category", "沟通风格", "description", "能够理解和感受他人情绪"),
                Map.of("id", "trait_007", "name", "耐心引导", "type", "personality", "category", "沟通风格", "description", "不急躁，愿意花时间解释和引导"),
                Map.of("id", "trait_008", "name", "创意发散", "type", "personality", "category", "语言语气", "description", "思维活跃，善于产生新颖想法"),
                Map.of("id", "trait_009", "name", "简洁表达", "type", "personality", "category", "语言语气", "description", "追求简洁明了的表达方式"),
                Map.of("id", "trait_010", "name", "代码质量优先", "type", "config", "category", "默认能力", "description", "严格遵循代码规范和最佳实践"),
                Map.of("id", "trait_011", "name", "性能意识", "type", "config", "category", "默认能力", "description", "关注系统性能和资源效率"),
                Map.of("id", "trait_012", "name", "学术严谨", "type", "config", "category", "默认能力", "description", "引用规范，内容经过验证"),
                Map.of("id", "trait_013", "name", "用户中心", "type", "personality", "category", "行事原则", "description", "始终以用户价值为出发点"),
                Map.of("id", "trait_014", "name", "迭代思维", "type", "personality", "category", "行事原则", "description", "小步快跑，持续改进"),
                Map.of("id", "trait_015", "name", "全渠道营销", "type", "config", "category", "工具白名单", "description", "覆盖多个营销渠道的整合能力"),
                Map.of("id", "trait_016", "name", "品牌叙事", "type", "personality", "category", "语言语气", "description", "擅长讲故事，建立情感连接")
        );

        // 过滤
        List<Map<String, Object>> filtered = mockTraits.stream()
                .filter(t -> {
                    if (search != null && !search.isBlank()) {
                        String s = search.toLowerCase();
                        boolean nameMatch = t.get("name") != null && t.get("name").toString().toLowerCase().contains(s);
                        boolean descMatch = t.get("description") != null && t.get("description").toString().toLowerCase().contains(s);
                        if (!nameMatch && !descMatch) return false;
                    }
                    if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
                        boolean typeMatch = t.get("type") != null && t.get("type").toString().equals(type);
                        if (!typeMatch) return false;
                    }
                    if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
                        boolean categoryMatch = t.get("category") != null && t.get("category").toString().equals(category);
                        if (!categoryMatch) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        List<Map<String, Object>> pageData = fromIndex >= filtered.size() ? List.of() : filtered.subList(fromIndex, toIndex);

        return ApiResponse.success(PageResponse.of(pageData, total, page, pageSize));
    }

    /**
     * 2.2 创建 Trait
     * POST /api/v1/studio/traits
     */
    @Operation(summary = "创建 Trait")
    @PostMapping("/traits")
    public ApiResponse<Map<String, Object>> createTrait(@RequestBody Map<String, Object> traitData) {
        // 临时实现：返回创建的 Trait
        Map<String, Object> response = new java.util.HashMap<>(traitData);
        response.put("id", "trait_" + System.currentTimeMillis());
        response.put("usedByCount", 0);
        response.put("enabled", true);
        response.put("createdAt", java.time.LocalDateTime.now().toString());
        response.put("updatedAt", java.time.LocalDateTime.now().toString());
        return ApiResponse.success(response);
    }

    /**
     * 2.3 获取 Trait 详情
     * GET /api/v1/studio/traits/:id
     */
    @Operation(summary = "获取 Trait 详情")
    @GetMapping("/traits/{id}")
    public ApiResponse<Map<String, Object>> getTrait(@PathVariable String id) {
        // 临时实现：返回 mock 数据
        return ApiResponse.success(Map.of(
                "id", id,
                "name", "示例 Trait",
                "type", "personality",
                "category", "沟通风格",
                "description", "这是一个示例 Trait",
                "content", "Trait 的详细内容",
                "usedByCount", 1,
                "enabled", true,
                "createdAt", java.time.LocalDateTime.now().minusDays(10).toString(),
                "updatedAt", java.time.LocalDateTime.now().minusDays(5).toString()
        ));
    }

    /**
     * 2.4 更新 Trait
     * PUT /api/v1/studio/traits/:id
     */
    @Operation(summary = "更新 Trait")
    @PutMapping("/traits/{id}")
    public ApiResponse<Map<String, Object>> updateTrait(
            @PathVariable String id,
            @RequestBody Map<String, Object> traitData) {
        // 临时实现：返回更新后的 Trait
        Map<String, Object> response = new java.util.HashMap<>(traitData);
        response.put("id", id);
        response.put("updatedAt", java.time.LocalDateTime.now().toString());
        return ApiResponse.success(response);
    }

    /**
     * 2.5 删除 Trait
     * DELETE /api/v1/studio/traits/:id
     */
    @Operation(summary = "删除 Trait")
    @DeleteMapping("/traits/{id}")
    public ApiResponse<Map<String, Object>> deleteTrait(@PathVariable String id) {
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 3. Template（内置模板）====================

    /**
     * 3.1 获取模板列表
     * GET /api/v1/studio/templates
     */
    @Operation(summary = "获取内置角色模板列表")
    @GetMapping("/templates")
    public ApiResponse<List<Map<String, Object>>> listTemplates(
            @RequestParam(required = false) String category) {
        // 临时实现：返回 mock 数据
        List<Map<String, Object>> mockTemplates = List.of(
                Map.of("id", "tpl_001", "name", "通用助手", "description", "适用于日常对话和任务协助的通用 AI 助手", "category", "助手", "scenario", "日常办公、个人助理", "preview", "一个友好、专业的 AI 助手，能够回答各种问题并协助完成日常任务。", "defaultTraits", List.of("trait_001", "trait_005")),
                Map.of("id", "tpl_002", "name", "数据分析师", "description", "专注于数据处理、分析和可视化的专业角色", "category", "分析", "scenario", "商业智能、数据报告", "preview", "专业的数据分析师，擅长从数据中提取洞察，生成清晰的可视化报告。", "defaultTraits", List.of("trait_002", "trait_004")),
                Map.of("id", "tpl_003", "name", "客服代表", "description", "温柔耐心的客户服务代表", "category", "客服", "scenario", "客户支持、问题解答", "preview", "耐心的客服代表，善于理解客户需求，提供贴心的解决方案。", "defaultTraits", List.of("trait_006", "trait_007")),
                Map.of("id", "tpl_004", "name", "创意写作者", "description", "富有创意的营销文案和内容创作者", "category", "创作", "scenario", "营销文案、内容创作", "preview", "创意十足的写作者，能够根据不同场景创作吸引人的文案内容。", "defaultTraits", List.of("trait_008", "trait_009")),
                Map.of("id", "tpl_005", "name", "代码审查员", "description", "严格的代码质量和性能审查专家", "category", "开发", "scenario", "代码审查、质量把控", "preview", "严格的代码审查员，关注代码质量、性能优化和最佳实践。", "defaultTraits", List.of("trait_010", "trait_011")),
                Map.of("id", "tpl_006", "name", "研究助手", "description", "严谨的学术研究辅助角色", "category", "研究", "scenario", "文献调研、学术写作", "preview", "专业的研究助手，擅长文献检索、信息整合和学术写作规范。", "defaultTraits", List.of("trait_012", "trait_004"))
        );

        // 过滤
        List<Map<String, Object>> filtered = mockTemplates.stream()
                .filter(t -> category == null || category.isBlank() || "all".equalsIgnoreCase(category) || t.get("category").toString().equals(category))
                .collect(Collectors.toList());

        return ApiResponse.success(filtered);
    }

    /**
     * 3.2 从模板创建角色
     * POST /api/v1/studio/templates/:id/derive
     */
    @Operation(summary = "从模板派生创建角色")
    @PostMapping("/templates/{id}/derive")
    public ApiResponse<Character> deriveCharacterFromTemplate(
            @PathVariable String id,
            @RequestBody DeriveTemplateRequest request) {
        // 临时实现：先以空白角色方式创建
        Character character = new Character();
        character.setName(request.getName());
        character.setDescription(request.getDescription());
        if (character.getExtensions() == null) {
            character.setExtensions(new java.util.HashMap<>());
        }
        character.getExtensions().put("source", "built_in_derived");
        character.getExtensions().put("templateId", id);

        // 根据模板设置默认 Traits
        List<String> defaultTraits = switch (id) {
            case "tpl_001" -> List.of("trait_001", "trait_005");
            case "tpl_002" -> List.of("trait_002", "trait_004");
            case "tpl_003" -> List.of("trait_006", "trait_007");
            case "tpl_004" -> List.of("trait_008", "trait_009");
            case "tpl_005" -> List.of("trait_010", "trait_011");
            case "tpl_006" -> List.of("trait_012", "trait_004");
            default -> List.of();
        };
        character.setTraits(defaultTraits);

        // 设置默认 Prompt 模板
        String defaultPrompt = switch (id) {
            case "tpl_001" -> "你是一位专业的 AI 助手，能够回答各种问题并协助完成日常任务。";
            case "tpl_002" -> "你是一位资深数据分析师，擅长使用统计学方法和可视化技术分析数据。";
            case "tpl_003" -> "你是一位热情的客服代表，始终以客户满意为首要目标。";
            case "tpl_004" -> "你是一位创意写作者，擅长用生动的语言和独特的视角创作内容。";
            case "tpl_005" -> "你是一位资深的代码审查员，对代码质量和性能有极高要求。";
            case "tpl_006" -> "你是一位专业的研究助手，帮助用户进行文献调研和信息整合。";
            default -> "你是一位专业的 AI 助手。";
        };
        character.setPromptTemplate(defaultPrompt);

        Character created = studioApplication.createCharacter(character);
        return ApiResponse.success(created);
    }

    // ==================== 4. Deployment（派驻记录）====================

    /**
     * 4.1 获取派驻记录列表
     * GET /api/v1/studio/deployments
     */
    @Operation(summary = "获取派驻记录列表")
    @GetMapping("/deployments")
    public ApiResponse<PageResponse<Map<String, Object>>> listDeployments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String characterId,
            @RequestParam(required = false) String workspaceId,
            @RequestParam(required = false) String status) {
        PageResponse<Map<String, Object>> result = studioApplication.listDeployments(
                page, pageSize, characterId, workspaceId);
        return ApiResponse.success(result);
    }

    /**
     * 4.2 派驻角色到 Workspace
     * POST /api/v1/studio/deployments
     */
    @Operation(summary = "派驻角色到 Workspace")
    @PostMapping("/deployments")
    public ApiResponse<Map<String, Object>> deployCharacter(@RequestBody DeployRequest request) {
        Map<String, Object> result = studioApplication.deployCharacter(
                request.getCharacterId(),
                request.getWorkspaceId(),
                request.getRole(),
                request.getPosition(),
                request.getLevel());
        return ApiResponse.success(result);
    }

    /**
     * 4.3 撤销派驻
     * DELETE /api/v1/studio/deployments/:id
     */
    @Operation(summary = "撤销派驻")
    @DeleteMapping("/deployments/{id}")
    public ApiResponse<Map<String, Object>> undeployCharacter(@PathVariable String id) {
        studioApplication.undeployCharacter(id);
        return ApiResponse.success(Map.of("success", true));
    }

    // ==================== 请求体 DTO ====================

    /** 角色运行请求 */
    @Data
    public static class RunCharacterRequest {
        /** 用户消息 */
        private String message;
        /** 会话 ID（可选） */
        private String sessionId;
    }

    /** 派驻请求 */
    @Data
    public static class DeployRequest {
        private String characterId;
        private String workspaceId;
        private String role;
        private String position;
        private Integer level;
    }

    /** 从模板派生请求 */
    @Data
    public static class DeriveTemplateRequest {
        private String name;
        private String description;
    }
}
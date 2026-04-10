package org.dragon.character.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.Character;
import org.dragon.character.service.CharacterService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * CharacterTemplateService 角色模板领域服务
 *
 * <p>提供角色模板的查询和派生功能。
 * 模板包含 skill、trait、personality、tool、memory 等配置。
 *
 * @author zhz
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterTemplateService {

    private final CharacterService characterService;

    /**
     * 内置角色模板定义
     */
    private static final List<Map<String, Object>> BUILT_IN_TEMPLATES = List.of(
            Map.of(
                    "id", "tpl_001",
                    "name", "通用助手",
                    "description", "适用于日常对话和任务协助的通用 AI 助手",
                    "category", "助手",
                    "scenario", "日常办公、个人助理",
                    "preview", "一个友好、专业的 AI 助手，能够回答各种问题并协助完成日常任务。",
                    "defaultTraits", List.of("trait_001", "trait_005"),
                    "defaultPrompt", "你是一位专业的 AI 助手，能够回答各种问题并协助完成日常任务。"
            ),
            Map.of(
                    "id", "tpl_002",
                    "name", "数据分析师",
                    "description", "专注于数据处理、分析和可视化的专业角色",
                    "category", "分析",
                    "scenario", "商业智能、数据报告",
                    "preview", "专业的数据分析师，擅长从数据中提取洞察，生成清晰的可视化报告。",
                    "defaultTraits", List.of("trait_002", "trait_004"),
                    "defaultPrompt", "你是一位资深数据分析师，擅长使用统计学方法和可视化技术分析数据。"
            ),
            Map.of(
                    "id", "tpl_003",
                    "name", "客服代表",
                    "description", "温柔耐心的客户服务代表",
                    "category", "客服",
                    "scenario", "客户支持、问题解答",
                    "preview", "耐心的客服代表，善于理解客户需求，提供贴心的解决方案。",
                    "defaultTraits", List.of("trait_006", "trait_007"),
                    "defaultPrompt", "你是一位热情的客服代表，始终以客户满意为首要目标。"
            ),
            Map.of(
                    "id", "tpl_004",
                    "name", "创意写作者",
                    "description", "富有创意的营销文案和内容创作者",
                    "category", "创作",
                    "scenario", "营销文案、内容创作",
                    "preview", "创意十足的写作者，能够根据不同场景创作吸引人的文案内容。",
                    "defaultTraits", List.of("trait_008", "trait_009"),
                    "defaultPrompt", "你是一位创意写作者，擅长用生动的语言和独特的视角创作内容。"
            ),
            Map.of(
                    "id", "tpl_005",
                    "name", "代码审查员",
                    "description", "严格的代码质量和性能审查专家",
                    "category", "开发",
                    "scenario", "代码审查、质量把控",
                    "preview", "严格的代码审查员，关注代码质量、性能优化和最佳实践。",
                    "defaultTraits", List.of("trait_010", "trait_011"),
                    "defaultPrompt", "你是一位资深的代码审查员，对代码质量和性能有极高要求。"
            ),
            Map.of(
                    "id", "tpl_006",
                    "name", "研究助手",
                    "description", "严谨的学术研究辅助角色",
                    "category", "研究",
                    "scenario", "文献调研、学术写作",
                    "preview", "专业的研究助手，擅长文献检索、信息整合和学术写作规范。",
                    "defaultTraits", List.of("trait_012", "trait_004"),
                    "defaultPrompt", "你是一位专业的研究助手，帮助用户进行文献调研和信息整合。"
            )
    );

    /**
     * 获取模板列表。
     *
     * @param category 分类筛选（null 表示全部）
     * @return 模板列表
     */
    public List<Map<String, Object>> listTemplates(String category) {
        if (category == null || category.isBlank() || "all".equalsIgnoreCase(category)) {
            return BUILT_IN_TEMPLATES;
        }
        return BUILT_IN_TEMPLATES.stream()
                .filter(t -> category.equalsIgnoreCase(String.valueOf(t.get("category"))))
                .toList();
    }

    /**
     * 根据 ID 获取模板。
     *
     * @param templateId 模板 ID
     * @return 模板（不存在返回 null）
     */
    public Map<String, Object> getTemplate(String templateId) {
        return BUILT_IN_TEMPLATES.stream()
                .filter(t -> templateId.equals(t.get("id")))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从模板派生创建角色。
     *
     * @param templateId 模板 ID
     * @param name        角色名称
     * @param description 角色描述
     * @return 创建的角色
     */
    public Character deriveCharacterFromTemplate(String templateId, String name, String description) {
        Map<String, Object> template = getTemplate(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateId);
        }

        Character character = new Character();
        character.setName(name);
        character.setDescription(description);
        if (character.getExtensions() == null) {
            character.setExtensions(new java.util.HashMap<>());
        }
        character.getExtensions().put("source", "built_in_derived");
        character.getExtensions().put("templateId", templateId);

        // 设置默认 Traits
        @SuppressWarnings("unchecked")
        List<String> defaultTraits = (List<String>) template.get("defaultTraits");
        character.setTraits(defaultTraits != null ? defaultTraits : List.of());

        // 设置默认 Prompt
        String defaultPrompt = (String) template.get("defaultPrompt");
        character.setPromptTemplate(defaultPrompt != null ? defaultPrompt : "你是一位专业的 AI 助手。");

        log.info("[CharacterTemplateService] Derived character from template: templateId={}, name={}", templateId, name);
        return characterService.createCharacter(character);
    }
}
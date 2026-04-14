package org.dragon.tool.runtime.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.ToolDefinition;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具声明构建器。
 *
 * <p>类比 {@code SkillDirectoryBuilder}，负责将 {@link ToolDefinition} 中的声明字段
 * 组装为 {@link UnifiedToolDeclaration}，供 {@link LlmToolAdapter} 转换为各 LLM 厂商格式。
 *
 * <p><b>字段映射关系</b>：
 * <pre>
 * ToolDefinition.toolName        → UnifiedToolDeclaration.name
 * ToolDefinition.toolDescription → UnifiedToolDeclaration.description
 * ToolDefinition.parameters      → UnifiedToolDeclaration.parameters  (JSON 反序列化)
 * ToolDefinition.requiredParams  → UnifiedToolDeclaration.required    (JSON 反序列化)
 * ToolDefinition.aliases         → UnifiedToolDeclaration.aliases     (JSON 反序列化)
 * </pre>
 *
 * <p><b>调用示例</b>（参考 {@code ToolExecutionService.buildToolDeclarations}）：
 * <pre>
 * UnifiedToolDeclaration decl = ToolDeclarationBuilder.build(version);
 * JsonNode formatted = adapter.toProviderFormat(decl);
 * </pre>
 */
@Slf4j
public class ToolDeclarationBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, ParameterSchema>> PARAMS_TYPE =
            new TypeReference<>() {};

    private static final TypeReference<List<String>> STRING_LIST_TYPE =
            new TypeReference<>() {};

    private ToolDeclarationBuilder() {}

    /**
     * 从 {@link ToolDefinition} 构建 {@link UnifiedToolDeclaration}。
     *
     * @param definition 工具版本领域对象（toolName、toolDescription 不可为空）
     * @return 构建好的统一工具声明，供 {@link LlmToolAdapter#toProviderFormat} 使用
     * @throws IllegalArgumentException 当 toolName 或 toolDescription 为空时
     */
    public static UnifiedToolDeclaration build(ToolDefinition definition) {
        if (!StringUtils.hasText(definition.getName())) {
            throw new IllegalArgumentException(
                    "toolName must not be empty, toolId=" + definition.getToolId());
        }
        if (!StringUtils.hasText(definition.getDescription())) {
            throw new IllegalArgumentException(
                    "toolDescription must not be empty, toolId=" + definition.getToolId());
        }

        return UnifiedToolDeclaration.builder()
                .name(definition.getName())
                .description(definition.getDescription())
                .parameters(parseParameters(definition.getParameters(), definition.getToolId()))
                .required(parseStringList(definition.getRequiredParams(), "requiredParams", definition.getToolId()))
                .aliases(definition.getAliases())
                .build();
    }

    // ── 私有解析方法 ────────────────────────────────────────────────────

    /**
     * 反序列化参数 Schema JSON 字符串为 {@code Map<String, ParameterSchema>}。
     * 为空时返回空 Map（允许无参工具）。
     */
    private static Map<String, ParameterSchema> parseParameters(String json, String toolId) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return MAPPER.readValue(json, PARAMS_TYPE);
        } catch (Exception e) {
            log.warn("[ToolDeclarationBuilder] 解析 parameters 失败: toolId={}, json={}, error={}",
                    toolId, json, e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * 反序列化 JSON 数组字符串为 {@code List<String>}。
     * 为空或解析失败时返回 null（区分"未设置"与"空列表"）。
     */
    private static List<String> parseStringList(String json, String fieldName, String toolId) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            log.warn("[ToolDeclarationBuilder] 解析 {} 失败: toolId={}, json={}, error={}",
                    fieldName, toolId, json, e.getMessage());
            return null;
        }
    }
}


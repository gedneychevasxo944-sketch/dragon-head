package org.dragon.tool.runtime.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工具参数的 Schema 定义，用于 {@link UnifiedToolDeclaration} 中描述单个参数。
 *
 * <p>对应 JSON Schema 规范的子集，覆盖 LLM 工具调用所需的全部参数描述能力：
 * <ul>
 *   <li>基本类型：string / integer / number / boolean</li>
 *   <li>复合类型：array（元素类型由 {@link #items} 描述）</li>
 *   <li>对象类型：object（属性由 {@link #properties} 描述）</li>
 *   <li>枚举约束：{@link #enumValues}（type=string 时的合法值列表）</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterSchema {

    /**
     * 参数类型。合法值：{@code "string"}、{@code "integer"}、{@code "number"}、
     * {@code "boolean"}、{@code "array"}、{@code "object"}。
     */
    private String type;

    /** 参数描述，供 LLM 理解参数含义 */
    private String description;

    /**
     * 枚举值列表。仅在 type = "string" 时有效，限定合法输入范围。
     * 例如：["asc", "desc"]
     */
    private List<String> enumValues;

    /**
     * 数组元素的 Schema。仅在 type = "array" 时有效。
     * 例如：数组中每个元素为 string 类型时，items.type = "string"
     */
    private ParameterSchema items;

    /**
     * 对象属性的 Schema Map。仅在 type = "object" 时有效。
     * key 为属性名，value 为该属性的 ParameterSchema。
     */
    private Map<String, ParameterSchema> properties;

    /**
     * 对象类型中的必填属性名列表。仅在 type = "object" 时有效。
     */
    private List<String> required;

    // ── 静态工厂方法 ──────────────────────────────────────────────

    /** 快速创建基本 string 类型参数（最常见用法） */
    public static ParameterSchema ofString(String description) {
        return builder().type("string").description(description).build();
    }

    /** 快速创建 boolean 类型参数 */
    public static ParameterSchema ofBoolean(String description) {
        return builder().type("boolean").description(description).build();
    }

    /** 快速创建 integer 类型参数 */
    public static ParameterSchema ofInteger(String description) {
        return builder().type("integer").description(description).build();
    }
}

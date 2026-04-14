package org.dragon.character.mind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 性格描述对象
 * 用于定义 Character 的性格特征、价值观、行为偏好等
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalityDescriptor {

    /**
     * Character 名称
     */
    private String name;

    /**
     * Character 描述
     */
    private String description;

    /**
     * 性格特征列表（存储完整 Trait 内容）
     */
    private List<TraitContent> traits;

    /**
     * 价值观列表
     */
    private List<String> values;

    /**
     * 沟通风格
     */
    private String communicationStyle;

    /**
     * 决策风格
     */
    private String decisionStyle;

    /**
     * 专业知识领域
     */
    private List<String> expertise;

    /**
     * 与其他 Character 的关系
     */
    private List<Relationship> relationships;

    /**
     * 扩展字段
     */
    private Map<String, Object> extensions;

    /**
     * Trait 内容详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraitContent {
        /**
         * Trait ID
         */
        private String id;
        /**
         * Trait 名称
         */
        private String name;
        /**
         * Trait 标签列表
         */
        private List<String> tags;
        /**
         * Trait 具体内容
         */
        private String content;
    }

    /**
     * 将性格描述转换为 LLM 系统 Prompt
     * 用于注入到 LLM 的 system message 中
     *
     * @return 格式化的 prompt 字符串
     */
    public String toPrompt() {
        StringBuilder prompt = new StringBuilder();

        if (name != null && !name.isEmpty()) {
            prompt.append("你是 ").append(name).append("。\n");
        }

        if (description != null && !description.isEmpty()) {
            prompt.append("你被描述为 ").append( description).append("\n");
        }

        if (traits != null && !traits.isEmpty()) {
            for (TraitContent trait : traits) {
                prompt.append("## ").append(trait.getName()).append("\n");
                prompt.append(trait.getContent()).append("\n\n");
            }
        }

        if (values != null && !values.isEmpty()) {
            prompt.append("价值观：").append(String.join("、", values)).append("。\n");
        }

        if (communicationStyle != null && !communicationStyle.isEmpty()) {
            prompt.append("沟通风格：").append(communicationStyle).append("。\n");
        }

        if (decisionStyle != null && !decisionStyle.isEmpty()) {
            prompt.append("决策风格：").append(decisionStyle).append("。\n");
        }

        if (expertise != null && !expertise.isEmpty()) {
            prompt.append("专业领域：").append(String.join("、", expertise)).append("。\n");
        }

        return prompt.toString();
    }

    /**
     * 关系描述
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Relationship {
        /**
         * 关联的 Character ID
         */
        private String characterId;

        /**
         * 印象描述
         */
        private String impression;

        /**
         * 关系类型
         */
        private String relationType;
    }
}

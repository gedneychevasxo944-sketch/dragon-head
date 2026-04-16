package org.dragon.workspace.step;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 输出 Schema，约束 LLM 必须按指定格式输出
 *
 * <p>通过 JSON Schema + Few-shot 示例，确保 LLM 输出结构化结果。
 *
 * @author yijunw
 */
@Data
@AllArgsConstructor
public class OutputSchema {

    /**
     * JSON Schema 描述期望的输出格式
     */
    private String jsonSchema;

    /**
     * Few-shot 示例
     */
    private List<String> fewShotExamples;

    /**
     * 创建步骤路由用的 Schema
     */
    public static OutputSchema forStepRouting() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "nextSteps": {
                  "type": "array",
                  "items": { "type": "string" },
                  "description": "要执行的 Step 名称列表"
                },
                "parameters": {
                  "type": "object",
                  "additionalProperties": { "type": "object" },
                  "description": "各 Step 的入参，key 为 stepName"
                },
                "loop": {
                  "type": "object",
                  "properties": {
                    "enabled": { "type": "boolean" },
                    "condition": { "type": "string" },
                    "maxIterations": { "type": "integer" }
                  }
                },
                "terminate": { "type": "boolean" }
              },
              "required": ["nextSteps"]
            }
            """;
        return new OutputSchema(schema, List.of());
    }

    /**
     * 创建成员选择用的 Schema
     */
    public static OutputSchema forMemberSelection() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "selectedCharacterId": {
                  "type": "string",
                  "description": "选中的 Character ID"
                },
                "reason": {
                  "type": "string",
                  "description": "选择原因"
                }
              },
              "required": ["selectedCharacterId"]
            }
            """;
        return new OutputSchema(schema, List.of());
    }
}

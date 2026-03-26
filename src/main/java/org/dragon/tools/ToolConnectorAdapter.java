package org.dragon.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.dragon.agent.tool.ToolConnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ToolConnector 适配器
 * 将 AgentTool 适配为 ToolConnector 接口
 * 包装异步执行为同步执行
 *
 * @author wyj
 * @version 1.0
 */
public class ToolConnectorAdapter implements ToolConnector {

    private final AgentTool agentTool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolConnectorAdapter(AgentTool agentTool) {
        this.agentTool = agentTool;
    }

    @Override
    public String getName() {
        return agentTool.getName();
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        try {
            // 将 Map 参数转为 JsonNode
            JsonNode paramsNode = objectMapper.valueToTree(params);

            AgentTool.ToolContext context = AgentTool.ToolContext.builder()
                    .parameters(paramsNode)
                    .build();

            // 同步等待异步执行结果
            AgentTool.ToolResult result = agentTool.execute(context)
                    .toCompletableFuture()
                    .get(30, TimeUnit.SECONDS);

            // 转换结果
            if (result.isSuccess()) {
                return ToolResult.builder()
                        .success(true)
                        .content(result.getOutput())
                        .data(result.getData() instanceof Map ? (Map<String, Object>) result.getData() : null)
                        .build();
            } else {
                return ToolResult.builder()
                        .success(false)
                        .errorMessage(result.getError())
                        .build();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Execution interrupted: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            return ToolResult.builder()
                    .success(false)
                    .errorMessage("Execution error: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public ToolSchema getSchema() {
        // 将 AgentTool 的 JSON Schema 转换为 ToolConnector 的 ToolSchema
        JsonNode paramSchema = agentTool.getParameterSchema();
        List<ToolParameter> params = new ArrayList<>();

        if (paramSchema != null && paramSchema.isObject() && paramSchema.has("properties")) {
            JsonNode properties = paramSchema.get("properties");
            JsonNode required = paramSchema.has("required") ? paramSchema.get("required") : objectMapper.createArrayNode();

            properties.fieldNames().forEachRemaining(fieldName -> {
                JsonNode fieldSchema = properties.get(fieldName);
                boolean isRequired = false;
                if (required.isArray()) {
                    for (JsonNode node : required) {
                        if (node.asText().equals(fieldName)) {
                            isRequired = true;
                            break;
                        }
                    }
                }
                ToolParameter param = ToolParameter.builder()
                        .name(fieldName)
                        .type(fieldSchema.has("type") ? fieldSchema.get("type").asText() : "string")
                        .description(fieldSchema.has("description") ? fieldSchema.get("description").asText() : null)
                        .required(isRequired)
                        .build();
                params.add(param);
            });
        }

        return ToolSchema.builder()
                .name(agentTool.getName())
                .description(agentTool.getDescription())
                .inputParameters(params)
                .build();
    }

    @Override
    public ToolType getType() {
        // AgentTool 没有 type 信息，默认为 CUSTOM
        return ToolType.CUSTOM;
    }

    /**
     * 获取被适配的 AgentTool
     */
    public AgentTool getAgentTool() {
        return agentTool;
    }
}
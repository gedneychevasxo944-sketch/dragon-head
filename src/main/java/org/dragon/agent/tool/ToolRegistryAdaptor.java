package org.dragon.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.dragon.tools.AgentTool;
import org.dragon.tools.ToolConnectorAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 工具注册中心（适配层）
 * 内部委托给 org.dragon.tools.ToolRegistry，向上层（ReActExecutor 等）提供 ToolConnector 接口
 * 通过 ToolConnectorAdapter 将 AgentTool 适配为 ToolConnector
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ToolRegistryAdaptor {

    private final org.dragon.tools.ToolRegistry delegate;

    public ToolRegistryAdaptor(org.dragon.tools.ToolRegistry delegate) {
        this.delegate = delegate;
    }

    /**
     * 注册工具（接收 ToolConnector，直接注册到 delegate）
     */
    public void register(ToolConnector connector) {
        if (connector == null || connector.getName() == null) {
            throw new IllegalArgumentException("Connector or Connector name cannot be null");
        }
        delegate.register(new ToolConnectorAdapterToAgentTool(connector));
        log.info("[ToolRegistry] Registered tool via adapter: {}", connector.getName());
    }

    /**
     * 获取工具（从 delegate 获取 AgentTool，适配为 ToolConnector 返回）
     */
    public Optional<ToolConnector> get(String name) {
        return delegate.get(name).map(agentTool -> {
            // 如果已经是适配器，直接返回底层 connector
            if (agentTool instanceof ToolConnectorAdapterToAgentTool) {
                return ((ToolConnectorAdapterToAgentTool) agentTool).getConnector();
            }
            // 否则包装
            return new ToolConnectorAdapter(agentTool);
        });
    }

    /**
     * 获取所有工具
     */
    public List<ToolConnector> listAll() {
        return delegate.listAll().stream()
                .map(agentTool -> {
                    if (agentTool instanceof ToolConnectorAdapterToAgentTool) {
                        return ((ToolConnectorAdapterToAgentTool) agentTool).getConnector();
                    }
                    return new ToolConnectorAdapter(agentTool);
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据类型获取工具
     */
    public List<ToolConnector> listByType(ToolConnector.ToolType type) {
        return listAll().stream()
                .filter(c -> c.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有工具 Schema
     */
    public List<ToolConnector.ToolSchema> listSchemas() {
        return listAll().stream()
                .map(ToolConnector::getSchema)
                .collect(Collectors.toList());
    }

    /**
     * 检查工具是否存在
     */
    public boolean exists(String name) {
        return delegate.get(name).isPresent();
    }

    /**
     * 获取注册表大小
     */
    public int size() {
        return delegate.size();
    }

    /**
     * 将 ToolConnector 适配为 AgentTool 的内部适配器
     */
    private static class ToolConnectorAdapterToAgentTool implements AgentTool {

        private final ToolConnector connector;

        ToolConnectorAdapterToAgentTool(ToolConnector connector) {
            this.connector = connector;
        }

        ToolConnector getConnector() {
            return connector;
        }

        @Override
        public String getName() {
            return connector.getName();
        }

        @Override
        public String getDescription() {
            ToolConnector.ToolSchema schema = connector.getSchema();
            return schema != null && schema.getDescription() != null ? schema.getDescription() : "";
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode getParameterSchema() {
            ToolConnector.ToolSchema schema = connector.getSchema();
            if (schema == null || schema.getInputParameters() == null) {
                return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();
            }

            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var properties = objectMapper.createObjectNode();
            var requiredArray = objectMapper.createArrayNode();

            for (ToolConnector.ToolParameter param : schema.getInputParameters()) {
                var paramNode = objectMapper.createObjectNode();
                paramNode.put("type", param.getType() != null ? param.getType() : "string");
                if (param.getDescription() != null) {
                    paramNode.put("description", param.getDescription());
                }
                properties.set(param.getName(), paramNode);
                if (param.isRequired()) {
                    requiredArray.add(param.getName());
                }
            }

            var result = objectMapper.createObjectNode();
            result.put("type", "object");
            result.set("properties", properties);
            if (requiredArray.size() > 0) {
                result.set("required", requiredArray);
            }
            return result;
        }

        @Override
        public java.util.concurrent.CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
            // 将 JsonNode 参数转回 Map
            var params = new java.util.HashMap<String, Object>();
            if (context.getParameters() != null && context.getParameters().isObject()) {
                context.getParameters().fields().forEachRemaining(field -> {
                    params.put(field.getKey(), field.getValue());
                });
            }

            ToolConnector.ToolResult result = connector.execute(params);

            if (result.isSuccess()) {
                return java.util.concurrent.CompletableFuture.completedFuture(
                        AgentTool.ToolResult.ok(result.getContent(), result.getData()));
            } else {
                return java.util.concurrent.CompletableFuture.completedFuture(
                        AgentTool.ToolResult.fail(result.getErrorMessage()));
            }
        }
    }
}
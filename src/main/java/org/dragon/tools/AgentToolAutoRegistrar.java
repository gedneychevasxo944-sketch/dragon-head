package org.dragon.tools;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AgentTool 自动注册器
 * 在应用启动时自动扫描并注册所有 AgentTool Bean 到 ToolRegistry
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentToolAutoRegistrar {

    private final ToolRegistry toolRegistry;
    private final List<AgentTool> agentTools;

    @PostConstruct
    public void registerTools() {
        int count = 0;
        for (AgentTool tool : agentTools) {
            if (tool != null && tool.getName() != null) {
                toolRegistry.register(tool);
                count++;
                log.info("[AgentToolAutoRegistrar] Registered tool: {}", tool.getName());
            }
        }
        log.info("[AgentToolAutoRegistrar] Total registered {} tools", count);
    }
}
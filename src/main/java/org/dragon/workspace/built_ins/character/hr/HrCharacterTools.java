package org.dragon.workspace.built_ins.character.hr;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.tools.ToolConnectorAdapter;
import org.dragon.tools.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * HR Character 工具工厂
 * 提供 HR Character 可用的工具列表
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class HrCharacterTools {

    private final ToolRegistry toolRegistry;

    public HrCharacterTools(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 获取 HR Character 可用的工具列表
     *
     * @return 工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        return java.util.Arrays.asList(
                toolRegistry.get("hire_character").map(ToolConnectorAdapter::new).orElse(null),
                toolRegistry.get("fire_character").map(ToolConnectorAdapter::new).orElse(null),
                toolRegistry.get("assign_duty").map(ToolConnectorAdapter::new).orElse(null),
                toolRegistry.get("list_candidates").map(ToolConnectorAdapter::new).orElse(null),
                toolRegistry.get("evaluate_character").map(ToolConnectorAdapter::new).orElse(null)
        );
    }

    /**
     * 检查指定工具是否可用
     *
     * @param toolName 工具名称
     * @return 是否可用
     */
    public boolean isToolAvailable(String toolName) {
        return toolRegistry.get(toolName).isPresent();
    }
}

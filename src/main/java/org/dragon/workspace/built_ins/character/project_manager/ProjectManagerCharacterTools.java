package org.dragon.workspace.built_ins.character.project_manager;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.tools.ToolConnectorAdapter;
import org.dragon.tools.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * ProjectManager Character 工具工厂
 * 提供 ProjectManager Character 可用的工具列表
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class ProjectManagerCharacterTools {

    private final ToolRegistry toolRegistry;

    public ProjectManagerCharacterTools(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 获取 ProjectManager Character 可用的工具列表
     *
     * @return 工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        return java.util.Arrays.asList(
                toolRegistry.get("decompose_task").map(ToolConnectorAdapter::new).orElse(null),
                toolRegistry.get("assign_subtask").map(ToolConnectorAdapter::new).orElse(null),
                toolRegistry.get("get_task_status").map(ToolConnectorAdapter::new).orElse(null)
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

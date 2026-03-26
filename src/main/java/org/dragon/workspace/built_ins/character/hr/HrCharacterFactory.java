package org.dragon.workspace.built_ins.character.hr;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.tools.ToolRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.AbstractWorkspaceCharacterFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * HR Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理 HR 功能的 Character 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class HrCharacterFactory extends AbstractWorkspaceCharacterFactory {

    private static final String HR_CHARACTER_PREFIX = "hr_";
    private static final String CHARACTER_TYPE = "hr";
    private static final String CHARACTER_NAME = "HR Manager";
    private static final String CHARACTER_DESCRIPTION = "负责 Workspace 的人力资源管理，包括招聘、解雇、职责分配等";

    private final ToolRegistry toolRegistry;

    public HrCharacterFactory(CharacterRegistry characterRegistry,
                              WorkspaceRegistry workspaceRegistry,
                              CharacterRuntimeBinder characterRuntimeBinder,
                              ToolRegistry toolRegistry) {
        super(characterRegistry, workspaceRegistry, characterRuntimeBinder);
        this.toolRegistry = toolRegistry;
    }

    @Override
    protected String getCharacterIdPrefix() {
        return HR_CHARACTER_PREFIX;
    }

    @Override
    public String getCharacterType() {
        return CHARACTER_TYPE;
    }

    @Override
    protected String getCharacterName() {
        return CHARACTER_NAME;
    }

    @Override
    protected String getCharacterDescription() {
        return CHARACTER_DESCRIPTION;
    }

    @Override
    public List<ToolConnector> getAvailableTools() {
        return java.util.Arrays.asList(
                toolRegistry.get("hire_character").map(agentTool -> new org.dragon.tools.ToolConnectorAdapter(agentTool)).orElse(null),
                toolRegistry.get("fire_character").map(agentTool -> new org.dragon.tools.ToolConnectorAdapter(agentTool)).orElse(null),
                toolRegistry.get("assign_duty").map(agentTool -> new org.dragon.tools.ToolConnectorAdapter(agentTool)).orElse(null),
                toolRegistry.get("list_candidates").map(agentTool -> new org.dragon.tools.ToolConnectorAdapter(agentTool)).orElse(null),
                toolRegistry.get("evaluate_character").map(agentTool -> new org.dragon.tools.ToolConnectorAdapter(agentTool)).orElse(null)
        );
    }

    /**
     * 获取 Workspace 的 HR Character (兼容旧接口)
     */
    public Character getOrCreateHrCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 HR Character (兼容旧接口)
     */
    public boolean hasHrCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

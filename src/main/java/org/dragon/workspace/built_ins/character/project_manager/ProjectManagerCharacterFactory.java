package org.dragon.workspace.built_ins.character.project_manager;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.AbstractWorkspaceCharacterFactory;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * ProjectManager Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理项目经理功能的 Character 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ProjectManagerCharacterFactory extends AbstractWorkspaceCharacterFactory {

    private static final String PROJECT_MANAGER_CHARACTER_PREFIX = "project_manager_";
    private static final String CHARACTER_TYPE = "project_manager";
    private static final String CHARACTER_NAME = "Project Manager";
    private static final String CHARACTER_DESCRIPTION = "负责将复杂任务拆解为可执行的子任务，并管理任务执行进度";

    private final ProjectManagerCharacterTools projectManagerCharacterTools;

    public ProjectManagerCharacterFactory(CharacterRegistry characterRegistry,
                                          WorkspaceRegistry workspaceRegistry,
                                          CharacterRuntimeBinder characterRuntimeBinder,
                                          ProjectManagerCharacterTools projectManagerCharacterTools) {
        super(characterRegistry, workspaceRegistry, characterRuntimeBinder);
        this.projectManagerCharacterTools = projectManagerCharacterTools;
    }

    @Override
    protected String getCharacterIdPrefix() {
        return PROJECT_MANAGER_CHARACTER_PREFIX;
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
        return projectManagerCharacterTools.getAvailableTools();
    }

    /**
     * 获取 Workspace 的 ProjectManager Character
     */
    public Character getOrCreateProjectManagerCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 ProjectManager Character
     */
    public boolean hasProjectManagerCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

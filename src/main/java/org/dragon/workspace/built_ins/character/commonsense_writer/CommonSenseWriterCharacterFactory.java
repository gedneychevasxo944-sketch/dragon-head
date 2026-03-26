package org.dragon.workspace.built_ins.character.commonsense_writer;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.CharacterRuntimeBinder;
import org.dragon.tools.ToolRegistry;
import org.dragon.workspace.WorkspaceRegistry;
import org.dragon.workspace.built_ins.character.AbstractWorkspaceCharacterFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CommonSenseWriter Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理 CommonSense 编写功能的 Character 实例
 * CommonSenseWriter 负责将 CommonSense 转换成 prompt
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class CommonSenseWriterCharacterFactory extends AbstractWorkspaceCharacterFactory {

    private static final String COMMON_SENSE_WRITER_CHARACTER_PREFIX = "commonsense_writer_";
    private static final String CHARACTER_TYPE = "commonsense_writer";
    private static final String CHARACTER_NAME = "Common Sense Writer";
    private static final String CHARACTER_DESCRIPTION = "负责将 CommonSense 常识转换成 prompt 格式";

    private final CommonSenseWriterCharacterTools commonSenseWriterCharacterTools;

    public CommonSenseWriterCharacterFactory(CharacterRegistry characterRegistry,
                                             WorkspaceRegistry workspaceRegistry,
                                             CharacterRuntimeBinder characterRuntimeBinder,
                                             CommonSenseWriterCharacterTools commonSenseWriterCharacterTools) {
        super(characterRegistry, workspaceRegistry, characterRuntimeBinder);
        this.commonSenseWriterCharacterTools = commonSenseWriterCharacterTools;
    }

    @Override
    protected String getCharacterIdPrefix() {
        return COMMON_SENSE_WRITER_CHARACTER_PREFIX;
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
        return commonSenseWriterCharacterTools.getAvailableTools();
    }

    /**
     * 获取 Workspace 的 CommonSenseWriter Character
     */
    public Character getOrCreateCommonSenseWriterCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 CommonSenseWriter Character
     */
    public boolean hasCommonSenseWriterCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

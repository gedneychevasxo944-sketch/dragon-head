package org.dragon.workspace.built_ins.character.member_selector;

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
 * MemberSelector Character 工厂
 * 实现 CharacterFactory 接口，为 Workspace 创建和管理成员选择功能的 Character 实例
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class MemberSelectorCharacterFactory extends AbstractWorkspaceCharacterFactory {

    private static final String MEMBER_SELECTOR_CHARACTER_PREFIX = "member_selector_";
    private static final String CHARACTER_TYPE = "member_selector";
    private static final String CHARACTER_NAME = "Member Selector";
    private static final String CHARACTER_DESCRIPTION = "负责从 Workspace 中已雇佣的 Character 中选择最合适的执行者来完成特定任务";

    private final MemberSelectorCharacterTools memberSelectorCharacterTools;

    public MemberSelectorCharacterFactory(CharacterRegistry characterRegistry,
                                        WorkspaceRegistry workspaceRegistry,
                                        CharacterRuntimeBinder characterRuntimeBinder,
                                        MemberSelectorCharacterTools memberSelectorCharacterTools) {
        super(characterRegistry, workspaceRegistry, characterRuntimeBinder);
        this.memberSelectorCharacterTools = memberSelectorCharacterTools;
    }

    @Override
    protected String getCharacterIdPrefix() {
        return MEMBER_SELECTOR_CHARACTER_PREFIX;
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
        return memberSelectorCharacterTools.getAvailableTools();
    }

    /**
     * 获取 Workspace 的 MemberSelector Character
     */
    public Character getOrCreateMemberSelectorCharacter(String workspaceId) {
        return getOrCreateCharacter(workspaceId);
    }

    /**
     * 检查 Workspace 是否有 MemberSelector Character
     */
    public boolean hasMemberSelectorCharacter(String workspaceId) {
        return hasCharacter(workspaceId);
    }
}

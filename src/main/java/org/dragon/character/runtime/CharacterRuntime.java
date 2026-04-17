package org.dragon.character.runtime;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.llm.caller.LLMCallerSelector;
import org.dragon.asset.service.AssetAssociationService;
import org.dragon.character.mind.Mind;
import org.dragon.character.mind.TraitResolutionService;
import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.tools.ToolRegistry;
import org.dragon.workspace.cooperation.chat.ChatRoom;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Builder;
import lombok.Data;

/**
 * Character 运行时依赖
 * 包含执行所需的外部服务依赖
 *
 * @author wyj
 * @version 1.0
 */
@Data
@Builder
public class CharacterRuntime {

    /**
     * LLM 调用选择器
     */
    @JsonIgnore
    private LLMCallerSelector callerSelector;

    /**
     * 模型注册中心
     */
    @JsonIgnore
    private ModelRegistry modelRegistry;

    /**
     * 工具注册中心
     */
    @JsonIgnore
    private ToolRegistry toolRegistry;

    /**
     * Prompt 管理器
     */
    @JsonIgnore
    private ConfigApplication configApplication;

    /**
     * Mind 实例
     */
    @JsonIgnore
    private Mind mind;

    /**
     * 工作空间 ID
     */
    @Builder.Default
    private Long workspaceId = null;

    /**
     * Skill 注册中心
     */
    @JsonIgnore
    private SkillRegistry skillRegistry;

    /**
     * Trait 解析服务
     */
    @JsonIgnore
    private TraitResolutionService traitResolutionService;

    /**
     * 资产关联服务
     */
    @JsonIgnore
    private AssetAssociationService assetAssociationService;

    /**
     * ChatRoom 聊天室服务
     */
    @JsonIgnore
    private ChatRoom chatRoom;
}
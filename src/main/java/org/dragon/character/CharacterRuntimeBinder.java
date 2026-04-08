package org.dragon.character;

import java.util.HashSet;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.react.ReActExecutor;
import org.dragon.agent.workflow.WorkflowExecutor;
import org.dragon.agent.workflow.WorkflowStore;
import org.dragon.character.mind.TraitResolutionService;
import org.dragon.store.StoreFactory;
import org.dragon.character.runtime.CharacterRuntime;
import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.runtime.SkillRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Character 运行时依赖绑定器
 * 负责在 Character 创建后注入运行时依赖
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterRuntimeBinder {

    private final ConfigApplication configApplication;
    private final ObjectProvider<ReActExecutor> reActExecutorProvider;
    private final WorkflowExecutor workflowExecutor;
    private final ModelRegistry modelRegistry;
    private final OrchestrationService orchestrationService;
    private final StoreFactory storeFactory;
    private final SkillRegistry skillRegistry;
    private final TraitResolutionService traitResolutionService;

    /**
     * 绑定 Character 运行时依赖
     */
    public void bind(Character character, String workspaceId) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }

        // 构建运行时依赖
        CharacterRuntime runtime = CharacterRuntime.builder()
                .configApplication(configApplication)
                .reActExecutor(reActExecutorProvider.getObject())
                .workflowExecutor(workflowExecutor)
                .workflowStore(getWorkflowStore())
                .modelRegistry(modelRegistry)
                .orchestrationService(orchestrationService)
                .mind(null)
                .workspaceId(workspaceId != null ? Long.parseLong(workspaceId) : null)
                .skillRegistry(skillRegistry)
                .traitResolutionService(traitResolutionService)
                .build();

        character.setRuntime(runtime);

        // 如果没有 allowedTools，初始化为空集合
        if (character.getAllowedTools() == null) {
            character.setAllowedTools(new HashSet<>());
        }

        log.info("[CharacterRuntimeBinder] Bound runtime dependencies for character {} in workspace {}",
                character.getId(), workspaceId);
    }

    private WorkflowStore getWorkflowStore() {
        return storeFactory.get(WorkflowStore.class);
    }
}

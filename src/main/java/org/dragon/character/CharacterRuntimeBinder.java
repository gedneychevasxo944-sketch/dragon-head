package org.dragon.character;

import java.util.HashSet;

import org.dragon.agent.model.ModelRegistry;
import org.dragon.agent.orchestration.OrchestrationService;
import org.dragon.agent.react.ReActExecutor;
import org.dragon.agent.workflow.WorkflowExecutor;
import org.dragon.agent.workflow.WorkflowStore;
import org.dragon.character.mind.Mind;
import org.dragon.store.StoreFactory;
import org.dragon.character.runtime.CharacterRuntime;
import org.dragon.config.PromptManager;
import org.dragon.skill.registry.SkillRegistry;
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

    private final PromptManager promptManager;
    private final ObjectProvider<ReActExecutor> reActExecutorProvider;
    private final WorkflowExecutor workflowExecutor;
    private final ModelRegistry modelRegistry;
    private final OrchestrationService orchestrationService;
    private final StoreFactory storeFactory;
    private final SkillRegistry skillRegistry;

    /**
     * 绑定 Character 运行时依赖
     */
    public void bind(Character character, String workspaceId) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }

        // 构建运行时依赖
        CharacterRuntime runtime = CharacterRuntime.builder()
                .promptManager(promptManager)
                .reActExecutor(reActExecutorProvider.getObject())
                .workflowExecutor(workflowExecutor)
                .workflowStore(getWorkflowStore())
                .modelRegistry(modelRegistry)
                .orchestrationService(orchestrationService)
                .mind(null)
                .workspaceId(workspaceId != null ? Long.parseLong(workspaceId.toString()) : null)
                .skillRegistry(skillRegistry)
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

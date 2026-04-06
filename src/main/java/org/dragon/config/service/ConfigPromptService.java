package org.dragon.config.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dragon.config.context.InheritanceContext;
import org.dragon.config.enums.ConfigLevel;
import org.dragon.config.store.ConfigStore;
import org.springframework.stereotype.Service;

/**
 * Prompt 管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigPromptService {

    private final ConfigStore configStore;
    private final ConfigEffectService configEffectService;

    /**
     * 获取 Prompt - 使用继承链查询
     *
     * @param promptKey prompt键 (如 "observer.suggestion")
     * @param context 继承上下文
     * @return 找到的prompt或null
     */
    public String getPrompt(String promptKey, InheritanceContext context) {
        ConfigEffectService.EffectiveConfig ec = configEffectService.getEffectiveConfig("prompt/" + promptKey, context);
        if (ec != null && ec.getEffectiveValue() != null) {
            return (String) ec.getEffectiveValue();
        }
        return null;
    }

    /**
     * 获取全局默认 Prompt
     *
     * @param promptKey prompt键
     * @param defaultValue 默认值
     * @return prompt或默认值
     */
    public String getGlobalPrompt(String promptKey, String defaultValue) {
        String value = getPrompt(promptKey, InheritanceContext.builder().level(ConfigLevel.GLOBAL_WORKSPACE).build());
        return value != null ? value : defaultValue;
    }

    /**
     * 获取 Prompt（4 级层级查找）
     *
     * <p>优先级: workspace+character > character > workspace > global
     */
    public String getPrompt(String workspace, String characterId, String promptKey) {
        // 1. 最高优先级: workspace+character 级别
        if (workspace != null && characterId != null) {
            InheritanceContext ctx = InheritanceContext.forGlobalWsChar(workspace, characterId);
            String value = getPrompt("prompt/" + promptKey, ctx);
            if (value != null) {
                return value;
            }
        }

        // 2. character 级别 (独立于 workspace)
        if (characterId != null) {
            InheritanceContext ctx = InheritanceContext.forGlobalCharacter(characterId);
            String value = getPrompt("prompt/" + promptKey, ctx);
            if (value != null) {
                return value;
            }
        }

        // 3. workspace 级别
        if (workspace != null) {
            InheritanceContext ctx = InheritanceContext.forGlobalWorkspace(workspace);
            String value = getPrompt("prompt/" + promptKey, ctx);
            if (value != null) {
                return value;
            }
        }

        // 4. global 级别 (最低优先级)
        return getGlobalPrompt(promptKey, null);
    }

    /**
     * 获取 Workspace 级别 Prompt
     */
    public String getWorkspacePrompt(String workspace, String promptKey) {
        return getPrompt(workspace, null, promptKey);
    }

    /**
     * 获取 Workspace 级别 Prompt（带默认值）
     */
    public String getWorkspacePrompt(String workspace, String promptKey, String defaultValue) {
        String value = getWorkspacePrompt(workspace, promptKey);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置全局 Prompt
     */
    public void setGlobalPrompt(String promptKey, String content) {
        configStore.set(ConfigLevel.GLOBAL_WORKSPACE, null, null, null, null, null, "prompt/" + promptKey, content);
        log.info("[ConfigPromptService] Set global prompt: {}", promptKey);
    }

    /**
     * 设置 Workspace 级别 Prompt
     */
    public void setWorkspacePrompt(String workspace, String promptKey, String content) {
        configStore.set(ConfigLevel.GLOBAL_WORKSPACE, workspace, null, null, null, null, "prompt/" + promptKey, content);
        log.info("[ConfigPromptService] Set workspace prompt: workspace={}, key={}", workspace, promptKey);
    }

    /**
     * 设置 Workspace+Character 级别 Prompt
     */
    public void setWorkspaceCharacterPrompt(String workspace, String characterId, String promptKey, String content) {
        configStore.set(ConfigLevel.GLOBAL_WS_CHAR, workspace, characterId, null, null, null, "prompt/" + promptKey, content);
        log.info("[ConfigPromptService] Set workspace+character prompt: workspace={}, char={}, key={}", workspace, characterId, promptKey);
    }
}
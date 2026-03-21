package org.dragon.workspace.built_ins.character.prompt_writer;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.springframework.stereotype.Component;

/**
 * PromptWriter Character 工具类
 * 提供 PromptWriter Character 可用的工具列表
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class PromptWriterCharacterTools {

    public PromptWriterCharacterTools() {
        // PromptWriter 目前不需要额外工具，prompt 模板拼接由 LLM 直接处理
    }

    /**
     * 获取可用的工具列表
     *
     * @return 工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        // PromptWriter 目前不需要额外工具
        return List.of();
    }
}

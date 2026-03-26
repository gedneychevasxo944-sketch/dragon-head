package org.dragon.workspace.built_ins.character.material_summary;

import java.util.List;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.workspace.material.MaterialStore;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MaterialSummaryCharacter 工具集
 * 提供物料摘要生成所需的工具
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialSummaryCharacterTools {

    private final MaterialStore materialStore;

    /**
     * 获取可用工具列表
     */
    public List<ToolConnector> getAvailableTools() {
        return List.of();
    }
}
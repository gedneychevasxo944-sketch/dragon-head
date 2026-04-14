package org.dragon.tool.runtime.tools.character.observer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.CharacterObservationSnapshot;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * GetCharacterState 工具
 * 获取 Character 的当前状态
 */
@Slf4j
@Component
public class GetCharacterStateTool extends AbstractTool<JsonNode, String> {

    private final DataCollector dataCollector;

    @Autowired
    public GetCharacterStateTool(DataCollector dataCollector) {
        super("get_character_state",
                "获取 Character 的当前状态，包括基本信息、任务统计、技能标签等",
                JsonNode.class);
        this.dataCollector = dataCollector;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String characterId = input.has("characterId") ? input.get("characterId").asText() : null;

                if (characterId == null || characterId.isEmpty()) {
                    return ToolResult.fail("Missing required parameter: characterId");
                }

                CharacterObservationSnapshot snapshot = dataCollector.collectCharacterObservation(characterId);
                if (snapshot == null) {
                    return ToolResult.fail("Character not found: " + characterId);
                }

                String json = objectMapper.writeValueAsString(snapshot);
                return ToolResult.ok(json, ToolResultBlockParam.ofText(context.getToolUseId(), json));

            } catch (Exception e) {
                log.error("[GetCharacterStateTool] Execution failed", e);
                return ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

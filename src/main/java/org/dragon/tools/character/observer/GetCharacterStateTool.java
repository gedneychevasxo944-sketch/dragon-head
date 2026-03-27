package org.dragon.tools.character.observer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.observer.collector.DataCollector;
import org.dragon.observer.collector.dto.CharacterObservationSnapshot;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * GetCharacterState 工具
 * 获取 Character 的当前状态
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetCharacterStateTool implements AgentTool {

    private final DataCollector dataCollector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_character_state";
    }

    @Override
    public String getDescription() {
        return "获取 Character 的当前状态，包括基本信息、任务统计、技能标签等";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode characterIdParam = properties.putObject("characterId");
        characterIdParam.put("type", "string");
        characterIdParam.put("description", "Character ID");

        schema.putArray("required").add("characterId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String characterId = params.has("characterId") ? params.get("characterId").asText() : null;

                if (characterId == null || characterId.isEmpty()) {
                    return AgentTool.ToolResult.fail("Missing required parameter: characterId");
                }

                CharacterObservationSnapshot snapshot = dataCollector.collectCharacterObservation(characterId);
                if (snapshot == null) {
                    return AgentTool.ToolResult.fail("Character not found: " + characterId);
                }

                return AgentTool.ToolResult.ok(objectMapper.writeValueAsString(snapshot), Map.of("snapshot", snapshot));

            } catch (Exception e) {
                log.error("[GetCharacterStateTool] Execution failed", e);
                return AgentTool.ToolResult.fail("Error: " + e.getMessage());
            }
        });
    }
}

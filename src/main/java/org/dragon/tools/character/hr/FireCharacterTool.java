package org.dragon.tools.character.hr;

import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.hiring.WorkspaceHiringService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * Fire Character 工具
 * 供 HR Character 使用，执行解雇操作
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class FireCharacterTool implements AgentTool {

    private final WorkspaceHiringService workspaceHiringService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "fire_character";
    }

    @Override
    public String getDescription() {
        return "Fire a character from a workspace. Use this tool when you need to remove a character from your team.";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "The workspace ID to fire the character from");

        ObjectNode characterIdParam = properties.putObject("characterId");
        characterIdParam.put("type", "string");
        characterIdParam.put("description", "The character ID to fire");

        schema.putArray("required").add("workspaceId").add("characterId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String workspaceId = params.has("workspaceId") ? params.get("workspaceId").asText() : null;
                String characterId = params.has("characterId") ? params.get("characterId").asText() : null;

                if (workspaceId == null || characterId == null) {
                    return AgentTool.ToolResult.fail("Missing required parameters: workspaceId or characterId");
                }

                // 执行解雇（使用 MANUAL 模式）
                workspaceHiringService.fire(workspaceId, characterId, HireMode.MANUAL);

                return AgentTool.ToolResult.ok("Successfully fired character " + characterId + " from workspace " + workspaceId);

            } catch (Exception e) {
                return AgentTool.ToolResult.fail("Failed to fire character: " + e.getMessage());
            }
        });
    }
}

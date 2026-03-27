package org.dragon.tools.character.hr;

import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.service.hiring.WorkspaceHiringService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * Hire Character 工具
 * 供 HR Character 使用，执行雇佣操作
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class HireCharacterTool implements AgentTool {

    private final WorkspaceHiringService workspaceHiringService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "hire_character";
    }

    @Override
    public String getDescription() {
        return "Hire a character to a workspace. Use this tool when you need to add a character to your team.";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "The workspace ID to hire the character into");

        ObjectNode characterIdParam = properties.putObject("characterId");
        characterIdParam.put("type", "string");
        characterIdParam.put("description", "The character ID to hire");

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

                // 执行雇佣（使用 MANUAL 模式，因为是 HR 执行）
                workspaceHiringService.hire(workspaceId, characterId, HireMode.MANUAL);

                return AgentTool.ToolResult.ok("Successfully hired character " + characterId + " to workspace " + workspaceId);

            } catch (Exception e) {
                return AgentTool.ToolResult.fail("Failed to hire character: " + e.getMessage());
            }
        });
    }
}

package org.dragon.tools.character.hr;

import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.workspace.service.hiring.WorkspaceHiringService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * Assign Duty 工具
 * 供 HR Character 使用，为 Character 分配职责描述
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class AssignDutyTool implements AgentTool {

    private final WorkspaceHiringService workspaceHiringService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "assign_duty";
    }

    @Override
    public String getDescription() {
        return "Assign a duty description to a character in a workspace. Use this tool to define what the character should do.";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "The workspace ID");

        ObjectNode characterIdParam = properties.putObject("characterId");
        characterIdParam.put("type", "string");
        characterIdParam.put("description", "The character ID");

        ObjectNode dutyDescriptionParam = properties.putObject("dutyDescription");
        dutyDescriptionParam.put("type", "string");
        dutyDescriptionParam.put("description", "The duty description for the character");

        schema.putArray("required").add("workspaceId").add("characterId").add("dutyDescription");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String workspaceId = params.has("workspaceId") ? params.get("workspaceId").asText() : null;
                String characterId = params.has("characterId") ? params.get("characterId").asText() : null;
                String dutyDescription = params.has("dutyDescription") ? params.get("dutyDescription").asText() : null;

                if (workspaceId == null || characterId == null || dutyDescription == null) {
                    return AgentTool.ToolResult.fail("Missing required parameters: workspaceId, characterId or dutyDescription");
                }

                // 设置职责描述
                workspaceHiringService.setCharacterDuty(workspaceId, characterId, dutyDescription);

                return AgentTool.ToolResult.ok("Successfully assigned duty to character " + characterId + " in workspace " + workspaceId);

            } catch (Exception e) {
                return AgentTool.ToolResult.fail("Failed to assign duty: " + e.getMessage());
            }
        });
    }
}

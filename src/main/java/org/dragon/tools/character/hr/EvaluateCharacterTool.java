package org.dragon.tools.character.hr;

import java.util.concurrent.CompletableFuture;

import org.dragon.asset.service.AssetAssociationService;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.tools.AgentTool;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * Evaluate Character 工具
 * 供 HR Character 使用，评估 Character 是否适合某个 Workspace
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class EvaluateCharacterTool implements AgentTool {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;
    private final AssetAssociationService assetAssociationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "evaluate_character";
    }

    @Override
    public String getDescription() {
        return "Evaluate a character's suitability for a workspace. Returns character information and compatibility assessment.";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode characterIdParam = properties.putObject("characterId");
        characterIdParam.put("type", "string");
        characterIdParam.put("description", "The character ID to evaluate");

        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "The workspace ID to evaluate against");

        schema.putArray("required").add("characterId").add("workspaceId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String characterId = params.has("characterId") ? params.get("characterId").asText() : null;
                String workspaceId = params.has("workspaceId") ? params.get("workspaceId").asText() : null;

                if (characterId == null || workspaceId == null) {
                    return AgentTool.ToolResult.fail("Missing required parameters: characterId or workspaceId");
                }

                // 验证 Character 存在
                Character character = characterRegistry.get(characterId)
                        .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

                // 验证 Workspace 存在
                workspaceRegistry.get(workspaceId)
                        .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

                // 构建评估结果
                StringBuilder content = new StringBuilder();
                content.append("Evaluation for character ").append(characterId).append(" in workspace ").append(workspaceId).append(":\n");
                content.append("- Name: ").append(character.getName()).append("\n");
                content.append("- Status: ").append(character.getStatus()).append("\n");
                content.append("- Description: ").append(character.getDescription() != null ? character.getDescription() : "N/A").append("\n");

                // 检查是否已在 workspace 中
                boolean isMember = assetAssociationService.getWorkspacesForCharacter(characterId).contains(workspaceId);
                content.append("- Already member: ").append(isMember).append("\n");

                return AgentTool.ToolResult.ok(content.toString());

            } catch (Exception e) {
                return AgentTool.ToolResult.fail("Failed to evaluate character: " + e.getMessage());
            }
        });
    }
}

package org.dragon.tool.runtime.tools.character.hr;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.workspace.WorkspaceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Evaluate Character 工具
 * 供 HR Character 使用，评估 Character 是否适合某个 Workspace
 */
@Slf4j
@Component
public class EvaluateCharacterTool extends AbstractTool<JsonNode, String> {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceRegistry workspaceRegistry;

    @Autowired
    public EvaluateCharacterTool(CharacterRegistry characterRegistry,
                                  WorkspaceRegistry workspaceRegistry) {
        super("evaluate_character",
                "Evaluate a character's suitability for a workspace. Returns character information and compatibility assessment.",
                JsonNode.class);
        this.characterRegistry = characterRegistry;
        this.workspaceRegistry = workspaceRegistry;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String characterId = input.has("characterId") ? input.get("characterId").asText() : null;
                String workspaceId = input.has("workspaceId") ? input.get("workspaceId").asText() : null;

                if (characterId == null || workspaceId == null) {
                    return ToolResult.fail("Missing required parameters: characterId or workspaceId");
                }

                Character character = characterRegistry.get(characterId)
                        .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

                workspaceRegistry.get(workspaceId)
                        .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));

                StringBuilder content = new StringBuilder();
                content.append("Evaluation for character ").append(characterId)
                        .append(" in workspace ").append(workspaceId).append(":\n");
                content.append("- Name: ").append(character.getName()).append("\n");
                content.append("- Status: ").append(character.getStatus()).append("\n");
                content.append("- Description: ")
                        .append(character.getDescription() != null ? character.getDescription() : "N/A").append("\n");

                boolean isMember = character.getWorkspaceIds() != null
                        && character.getWorkspaceIds().contains(workspaceId);
                content.append("- Already member: ").append(isMember).append("\n");

                String result = content.toString();
                return ToolResult.ok(result, ToolResultBlockParam.ofText(context.getToolUseId(), result));

            } catch (Exception e) {
                log.error("[EvaluateCharacterTool] Execution failed", e);
                return ToolResult.fail("Failed to evaluate character: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

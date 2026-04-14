package org.dragon.tool.runtime.tools.character.hr;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.workspace.hiring.HireMode;
import org.dragon.workspace.hiring.WorkspaceHiringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Fire Character 工具
 * 供 HR Character 使用，执行解雇操作
 */
@Slf4j
@Component
public class FireCharacterTool extends AbstractTool<JsonNode, String> {

    private final WorkspaceHiringService workspaceHiringService;

    @Autowired
    public FireCharacterTool(WorkspaceHiringService workspaceHiringService) {
        super("fire_character",
                "Fire a character from a workspace. Use this tool when you need to remove a character from your team.",
                JsonNode.class);
        this.workspaceHiringService = workspaceHiringService;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String workspaceId = input.has("workspaceId") ? input.get("workspaceId").asText() : null;
                String characterId = input.has("characterId") ? input.get("characterId").asText() : null;

                if (workspaceId == null || characterId == null) {
                    return ToolResult.fail("Missing required parameters: workspaceId or characterId");
                }

                workspaceHiringService.fire(workspaceId, characterId, HireMode.MANUAL);

                String msg = "Successfully fired character " + characterId + " from workspace " + workspaceId;
                return ToolResult.ok(msg, ToolResultBlockParam.ofText(context.getToolUseId(), msg));

            } catch (Exception e) {
                log.error("[FireCharacterTool] Execution failed", e);
                return ToolResult.fail("Failed to fire character: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }

    @Override
    public boolean isReadOnly(JsonNode input) {
        return false;
    }

    @Override
    public boolean isDestructive(JsonNode input) {
        return true;
    }
}

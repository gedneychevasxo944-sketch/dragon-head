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
 * Hire Character 工具
 * 供 HR Character 使用，执行雇佣操作
 */
@Slf4j
@Component
public class HireCharacterTool extends AbstractTool<JsonNode, String> {

    private final WorkspaceHiringService workspaceHiringService;

    @Autowired
    public HireCharacterTool(WorkspaceHiringService workspaceHiringService) {
        super("hire_character",
                "Hire a character to a workspace. Use this tool when you need to add a character to your team.",
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

                workspaceHiringService.hire(workspaceId, characterId, HireMode.MANUAL);

                String msg = "Successfully hired character " + characterId + " to workspace " + workspaceId;
                return ToolResult.ok(msg, ToolResultBlockParam.ofText(context.getToolUseId(), msg));

            } catch (Exception e) {
                log.error("[HireCharacterTool] Execution failed", e);
                return ToolResult.fail("Failed to hire character: " + e.getMessage());
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
}

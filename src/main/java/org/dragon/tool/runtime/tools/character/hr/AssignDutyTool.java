package org.dragon.tool.runtime.tools.character.hr;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.workspace.hiring.WorkspaceHiringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Assign Duty 工具
 * 供 HR Character 使用，为 Character 分配职责描述
 */
@Slf4j
@Component
public class AssignDutyTool extends AbstractTool<JsonNode, String> {

    private final WorkspaceHiringService workspaceHiringService;

    @Autowired
    public AssignDutyTool(WorkspaceHiringService workspaceHiringService) {
        super("assign_duty",
                "Assign a duty description to a character in a workspace. Use this tool to define what the character should do.",
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
                String dutyDescription = input.has("dutyDescription") ? input.get("dutyDescription").asText() : null;

                if (workspaceId == null || characterId == null || dutyDescription == null) {
                    return ToolResult.fail("Missing required parameters: workspaceId, characterId or dutyDescription");
                }

                workspaceHiringService.setCharacterDuty(workspaceId, characterId, dutyDescription);

                String msg = "Successfully assigned duty to character " + characterId + " in workspace " + workspaceId;
                return ToolResult.ok(msg, ToolResultBlockParam.ofText(context.getToolUseId(), msg));

            } catch (Exception e) {
                log.error("[AssignDutyTool] Execution failed", e);
                return ToolResult.fail("Failed to assign duty: " + e.getMessage());
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

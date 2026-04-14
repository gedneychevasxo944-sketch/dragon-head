package org.dragon.tool.runtime.tools.character.hr;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.dragon.workspace.member.WorkspaceMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * List Candidates 工具
 * 供 HR Character 使用，列出可用的 Character 候选人
 */
@Slf4j
@Component
public class ListCandidatesTool extends AbstractTool<JsonNode, String> {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceMemberService memberManagementService;

    @Autowired
    public ListCandidatesTool(CharacterRegistry characterRegistry,
                               WorkspaceMemberService memberManagementService) {
        super("list_candidates",
                "List available character candidates for a workspace. This shows characters that are not yet members of the workspace.",
                JsonNode.class);
        this.characterRegistry = characterRegistry;
        this.memberManagementService = memberManagementService;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String workspaceId = input.has("workspaceId") ? input.get("workspaceId").asText() : null;

                if (workspaceId == null) {
                    return ToolResult.fail("Missing required parameter: workspaceId");
                }

                List<String> currentMemberIds = memberManagementService.listMembers(workspaceId)
                        .stream()
                        .map(m -> m.getCharacterId())
                        .collect(Collectors.toList());

                List<Character> availableCharacters = characterRegistry.listAll().stream()
                        .filter(c -> !currentMemberIds.contains(c.getId()))
                        .filter(c -> c.getStatus() == CharacterProfile.Status.RUNNING)
                        .collect(Collectors.toList());

                StringBuilder content = new StringBuilder();
                content.append("Available candidates for workspace ").append(workspaceId).append(":\n");
                for (Character c : availableCharacters) {
                    content.append("- ").append(c.getId()).append(": ").append(c.getName());
                    if (c.getDescription() != null) {
                        content.append(" - ").append(c.getDescription());
                    }
                    content.append("\n");
                }

                String result = content.toString();
                return ToolResult.ok(result, ToolResultBlockParam.ofText(context.getToolUseId(), result));

            } catch (Exception e) {
                log.error("[ListCandidatesTool] Execution failed", e);
                return ToolResult.fail("Failed to list candidates: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

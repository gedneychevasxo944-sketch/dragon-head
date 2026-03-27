package org.dragon.tools.character.hr;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.dragon.tools.AgentTool;
import org.dragon.character.Character;
import org.dragon.character.CharacterRegistry;
import org.dragon.character.profile.CharacterProfile;
import org.dragon.workspace.service.member.WorkspaceMemberManagementService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * List Candidates 工具
 * 供 HR Character 使用，列出可用的 Character 候选人
 *
 * @author wyj
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ListCandidatesTool implements AgentTool {

    private final CharacterRegistry characterRegistry;
    private final WorkspaceMemberManagementService memberManagementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "list_candidates";
    }

    @Override
    public String getDescription() {
        return "List available character candidates for a workspace. This shows characters that are not yet members of the workspace.";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "The workspace ID to list candidates for");

        schema.putArray("required").add("workspaceId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String workspaceId = params.has("workspaceId") ? params.get("workspaceId").asText() : null;

                if (workspaceId == null) {
                    return AgentTool.ToolResult.fail("Missing required parameter: workspaceId");
                }

                // 获取当前 workspace 的成员
                java.util.List<String> currentMemberIds = memberManagementService.listMembers(workspaceId)
                        .stream()
                        .map(m -> m.getCharacterId())
                        .collect(Collectors.toList());

                // 获取所有可用 Character，排除已在 workspace 中的
                java.util.List<Character> availableCharacters = characterRegistry.listAll().stream()
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

                return AgentTool.ToolResult.ok(content.toString());

            } catch (Exception e) {
                return AgentTool.ToolResult.fail("Failed to list candidates: " + e.getMessage());
            }
        });
    }
}

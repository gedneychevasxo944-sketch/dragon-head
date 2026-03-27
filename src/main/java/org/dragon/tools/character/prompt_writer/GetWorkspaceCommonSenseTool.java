package org.dragon.tools.character.prompt_writer;

import java.util.concurrent.CompletableFuture;

import org.dragon.tools.AgentTool;
import org.dragon.workspace.commons.CommonSenseService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 获取 Workspace CommonSense Prompt 的工具
 * 供 PromptWriter Character 在 ReAct 过程中按需调用
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetWorkspaceCommonSenseTool implements AgentTool {

    private final CommonSenseService commonSenseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "get_workspace_common_sense";
    }

    @Override
    public String getDescription() {
        return "获取指定 Workspace 的 CommonSense Prompt。当需要了解工作空间的常识规则、约束条件、角色边界或长期目标时调用此工具。";
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode workspaceIdParam = properties.putObject("workspaceId");
        workspaceIdParam.put("type", "string");
        workspaceIdParam.put("description", "工作空间 ID");

        schema.putArray("required").add("workspaceId");

        return schema;
    }

    @Override
    public CompletableFuture<AgentTool.ToolResult> execute(AgentTool.ToolContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonNode params = context.getParameters();
                String workspaceId = params.has("workspaceId") ? params.get("workspaceId").asText() : null;

                if (workspaceId == null || workspaceId.isEmpty()) {
                    return AgentTool.ToolResult.fail("Missing required parameter: workspaceId");
                }

                log.info("[GetWorkspaceCommonSenseTool] Generating common sense prompt for workspace: {}", workspaceId);
                String prompt = commonSenseService.generatePrompt(workspaceId);

                return AgentTool.ToolResult.ok(prompt != null ? prompt : "");

            } catch (Exception e) {
                log.error("[GetWorkspaceCommonSenseTool] Failed to get common sense", e);
                return AgentTool.ToolResult.fail("Failed to get common sense: " + e.getMessage());
            }
        });
    }
}

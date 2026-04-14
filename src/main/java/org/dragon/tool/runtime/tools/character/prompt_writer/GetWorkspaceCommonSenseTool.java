package org.dragon.tool.runtime.tools.character.prompt_writer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.commonsense.CommonSenseService;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 获取 Workspace CommonSense Prompt 的工具
 * 供 PromptWriter Character 在 ReAct 过程中按需调用
 */
@Slf4j
@Component
public class GetWorkspaceCommonSenseTool extends AbstractTool<JsonNode, String> {

    private final CommonSenseService commonSenseService;

    @Autowired
    public GetWorkspaceCommonSenseTool(CommonSenseService commonSenseService) {
        super("get_workspace_common_sense",
                "获取指定 Workspace 的 CommonSense Prompt。当需要了解工作空间的常识规则、约束条件、角色边界或长期目标时调用此工具。",
                JsonNode.class);
        this.commonSenseService = commonSenseService;
    }

    @Override
    protected CompletableFuture<ToolResult<String>> doCall(JsonNode input,
                                                            ToolUseContext context,
                                                            Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String workspaceId = input.has("workspaceId") ? input.get("workspaceId").asText() : null;

                if (workspaceId == null || workspaceId.isEmpty()) {
                    return ToolResult.fail("Missing required parameter: workspaceId");
                }

                log.info("[GetWorkspaceCommonSenseTool] Generating common sense prompt for workspace: {}", workspaceId);
                String prompt = commonSenseService.generatePrompt(workspaceId);
                String result = prompt != null ? prompt : "";

                return ToolResult.ok(result, ToolResultBlockParam.ofText(context.getToolUseId(), result));

            } catch (Exception e) {
                log.error("[GetWorkspaceCommonSenseTool] Failed to get common sense", e);
                return ToolResult.fail("Failed to get common sense: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(String output, String toolUseId) {
        return ToolResultBlockParam.ofText(toolUseId, output != null ? output : "");
    }
}

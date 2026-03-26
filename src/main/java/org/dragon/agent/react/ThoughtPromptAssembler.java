package org.dragon.agent.react;

import java.util.List;
import java.util.Map;

import org.dragon.agent.tool.ToolConnector;
import org.dragon.tools.ToolConnectorAdapter;
import org.dragon.tools.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * Thought Prompt 装配器
 * 负责将 ReActContext 组装成完整的思考阶段 Prompt
 *
 * @author wyj
 * @version 1.0
 */
@Component
public class ThoughtPromptAssembler {

    private final ToolRegistry toolRegistry;

    public ThoughtPromptAssembler(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * 装配思考阶段的完整 Prompt
     */
    public String assemble(ReActContext context) {
        return buildLocalThoughtPrompt(context);
    }

    /**
     * 构建本地 Thought Prompt（回退方案）
     */
    private String buildLocalThoughtPrompt(ReActContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("用户输入: ").append(context.getUserInput()).append("\n\n");

        // 添加物料上下文（如果存在）
        if (context.getMaterialContext() != null && !context.getMaterialContext().isEmpty()) {
            prompt.append("## 物料信息\n");
            prompt.append(context.getMaterialContext()).append("\n\n");
        }

        // 添加历史记录
        if (!context.getThoughts().isEmpty()) {
            prompt.append("之前的思考:\n");
            for (int i = 0; i < context.getThoughts().size(); i++) {
                prompt.append(i + 1).append(". ").append(context.getThoughts().get(i)).append("\n");
            }
            prompt.append("\n");
        }

        if (!context.getActions().isEmpty()) {
            prompt.append("之前的动作:\n");
            for (int i = 0; i < context.getActions().size(); i++) {
                Action a = context.getActions().get(i);
                prompt.append(i + 1).append(". ").append(a.getType());
                if (a.getToolName() != null) {
                    prompt.append(": ").append(a.getToolName());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        if (!context.getObservations().isEmpty()) {
            prompt.append("观察结果:\n");
            for (int i = 0; i < context.getObservations().size(); i++) {
                prompt.append(i + 1).append(". ").append(context.getObservations().get(i)).append("\n");
            }
            prompt.append("\n");
        }

        // 添加协作上下文（如果启用）
        if (context.isCollaborationJudgementEnabled() && context.getCollaborationDecisionPrompt() != null) {
            prompt.append(appendCollaborationContext(context));
        }

        // 添加可用工具信息
        if (!context.getAllowedTools().isEmpty()) {
            prompt.append("## 可用工具\n");
            prompt.append("你可以使用以下工具来完成用户的请求：\n\n");
            for (String toolName : context.getAllowedTools()) {
                toolRegistry.get(toolName).map(agentTool -> new ToolConnectorAdapter(agentTool)).ifPresent(connector -> {
                    ToolConnector.ToolSchema schema = connector.getSchema();
                    prompt.append(String.format("- **%s**: %s\n",
                            schema.getName(),
                            schema.getDescription() != null ? schema.getDescription() : "无描述"));
                    if (schema.getInputParameters() != null && !schema.getInputParameters().isEmpty()) {
                        prompt.append("  参数:\n");
                        for (ToolConnector.ToolParameter param : schema.getInputParameters()) {
                            prompt.append(String.format("    - %s (%s): %s %s\n",
                                    param.getName(),
                                    param.getType(),
                                    param.getDescription() != null ? param.getDescription() : "",
                                    param.isRequired() ? "[必填]" : "[可选]"));
                        }
                    }
                });
            }
            prompt.append("\n");
        }

        prompt.append("请分析上述信息，给出下一步的行动。\n");
        prompt.append("请以 JSON 格式返回你的决策：\n");
        prompt.append("{\n");
        prompt.append("  \"action\": \"TOOL|RESPOND|FINISH|MEMORY|STATUS_CHANGE\",  // 动作类型\n");
        prompt.append("  \"tool\": \"工具名称\",  // TOOL 类型时必填\n");
        prompt.append("  \"params\": {\"key\": \"value\"}  // 可选参数\n");
        prompt.append("  \"response\": \"响应内容\"  // RESPOND/FINISH 类型时填写\n");
        prompt.append("  \"statusChange\": {  // STATUS_CHANGE 类型时填写\n");
        prompt.append("    \"targetStatus\": \"WAITING_DEPENDENCY|WAITING_USER_INPUT|SUSPENDED\",\n");
        prompt.append("    \"reason\": \"变更原因\",\n");
        prompt.append("    \"dependencyTaskId\": \"等待的依赖任务ID\",\n");
        prompt.append("    \"question\": \"需要用户回答的问题\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("说明：\n");
        prompt.append("- TOOL: 使用工具，tool 填写工具名称\n");
        prompt.append("- RESPOND: 直接回复用户\n");
        prompt.append("- FINISH: 完成任务并给出最终回复\n");
        prompt.append("- MEMORY: 搜索记忆，params 需要包含 query 字段\n");
        prompt.append("- STATUS_CHANGE: 状态变更，用于主动改变任务状态\n");

        return prompt.toString();
    }

    /**
     * 追加协作上下文段落
     */
    private String appendCollaborationContext(ReActContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 协作上下文\n");
        sb.append(context.getCollaborationDecisionPrompt()).append("\n\n");

        sb.append("当前协作会话 ID: ")
                .append(context.getCollaborationSessionId() != null ? context.getCollaborationSessionId() : "无").append("\n\n");

        sb.append("参与者状态:\n");
        if (context.getParticipantStates() != null && !context.getParticipantStates().isEmpty()) {
            context.getParticipantStates().forEach((charId, status) ->
                    sb.append("  - ").append(charId).append(": ").append(status).append("\n"));
        } else {
            sb.append("  无\n");
        }

        sb.append("\n阻塞中的参与者:\n");
        if (context.getBlockedParticipants() != null && !context.getBlockedParticipants().isEmpty()) {
            context.getBlockedParticipants().forEach(p -> sb.append("  - ").append(p).append("\n"));
        } else {
            sb.append("  无\n");
        }

        sb.append("\n协作会话状态: ")
                .append(context.getSessionStatus() != null ? context.getSessionStatus() : "未知").append("\n\n");

        sb.append("最近协作消息:\n");
        if (context.getLatestSessionMessages() != null && !context.getLatestSessionMessages().isEmpty()) {
            context.getLatestSessionMessages().forEach(msg -> sb.append("  - ").append(msg).append("\n"));
        } else {
            sb.append("  无\n");
        }

        sb.append("\n同级 Character IDs: ")
                .append(context.getPeerCharacterIds() != null ? String.join(", ", context.getPeerCharacterIds()) : "无").append("\n\n");

        sb.append("依赖任务 IDs: ")
                .append(context.getDependencyTaskIds() != null ? String.join(", ", context.getDependencyTaskIds()) : "无").append("\n\n");

        return sb.toString();
    }
}

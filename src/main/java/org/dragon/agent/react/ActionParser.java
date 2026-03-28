package org.dragon.agent.react;

import java.util.Map;

import org.dragon.agent.react.Action.ActionType;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Action 解析器
 * 负责将 LLM 响应解析为 Action 对象
 * 优先 JSON 解析，回退到关键词匹配
 *
 * @author wyj
 * @version 1.0
 */
@Slf4j
@Component
public class ActionParser {

    private final Gson gson = new Gson();

    /**
     * 解析动作
     * 优先尝试 JSON 解析，回退到关键词匹配
     *
     * @param thought LLM 响应
     * @return 动作
     */
    public Action parse(String thought) {
        // 优先尝试 JSON 解析
        Action jsonAction = parseJsonAction(thought);
        if (jsonAction != null) {
            return jsonAction;
        }

        // 回退到关键词匹配
        return parseKeywordAction(thought);
    }

    /**
     * 尝试从 JSON 格式解析动作
     * 期望格式: {"action": "TOOL|RESPOND|FINISH|MEMORY|STATUS_CHANGE", "tool": "xxx", "params": {...}, "response": "...", "modelId": "...", "statusChange": "..."}
     */
    private Action parseJsonAction(String thought) {
        try {
            // 尝试提取 JSON 对象
            String jsonStr = extractJson(thought);
            if (jsonStr == null) {
                return null;
            }

            JsonObject json = gson.fromJson(jsonStr, JsonObject.class);

            if (!json.has("action")) {
                return null;
            }

            String actionType = json.get("action").getAsString().toUpperCase();
            ActionType type = switch (actionType) {
                case "TOOL" -> ActionType.TOOL;
                case "RESPOND" -> ActionType.RESPOND;
                case "FINISH" -> ActionType.FINISH;
                case "MEMORY" -> ActionType.MEMORY;
                case "STATUS_CHANGE" -> ActionType.STATUS_CHANGE;
                default -> null;
            };

            if (type == null) {
                return null;
            }

            Action.ActionBuilder builder = Action.builder().type(type);

            if (json.has("tool")) {
                builder.toolName(json.get("tool").getAsString());
            }

            if (json.has("params")) {
                Map<String, Object> params = gson.fromJson(json.get("params"), Map.class);
                builder.parameters(params);
            }

            if (json.has("response")) {
                builder.response(json.get("response").getAsString());
            }

            if (json.has("modelId")) {
                builder.modelId(json.get("modelId").getAsString());
            }

            if (json.has("statusChange")) {
                JsonObject statusChangeJson = json.getAsJsonObject("statusChange");
                Action.StatusChange statusChange = Action.StatusChange.builder()
                        .targetStatus(statusChangeJson.has("targetStatus") ? statusChangeJson.get("targetStatus").getAsString() : null)
                        .reason(statusChangeJson.has("reason") ? statusChangeJson.get("reason").getAsString() : null)
                        .dependencyTaskId(statusChangeJson.has("dependencyTaskId") ? statusChangeJson.get("dependencyTaskId").getAsString() : null)
                        .question(statusChangeJson.has("question") ? statusChangeJson.get("question").getAsString() : null)
                        .build();
                builder.statusChange(statusChange);
            }

            return builder.build();

        } catch (JsonSyntaxException | IllegalStateException e) {
            log.debug("[ActionParser] JSON 解析失败，回退到关键词匹配");
            return null;
        }
    }

    /**
     * 从文本中提取 JSON 对象
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    /**
     * 通过关键词匹配解析动作（回退方案）
     */
    private Action parseKeywordAction(String thought) {
        if (thought.contains("FINISH") || thought.contains("完成")) {
            return Action.builder()
                    .type(ActionType.FINISH)
                    .build();
        }

        if (thought.contains("TOOL:") || thought.contains("工具:")) {
            String toolName = extractToolName(thought);
            return Action.builder()
                    .type(ActionType.TOOL)
                    .toolName(toolName)
                    .build();
        }

        // 默认为响应动作
        return Action.builder()
                .type(ActionType.RESPOND)
                .toolName(thought)
                .build();
    }

    /**
     * 从思考中提取工具名称
     */
    private String extractToolName(String thought) {
        // 简单实现：查找 TOOL: 后面的内容
        int index = thought.indexOf("TOOL:");
        if (index >= 0) {
            String after = thought.substring(index + 5).trim();
            int spaceIndex = after.indexOf(' ');
            if (spaceIndex > 0) {
                return after.substring(0, spaceIndex);
            }
            return after;
        }

        index = thought.indexOf("工具:");
        if (index >= 0) {
            String after = thought.substring(index + 3).trim();
            int spaceIndex = after.indexOf(' ');
            if (spaceIndex > 0) {
                return after.substring(0, spaceIndex);
            }
            return after;
        }

        return null;
    }
}

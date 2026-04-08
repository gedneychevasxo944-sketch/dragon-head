package org.dragon.tools.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dragon.skill.runtime.*;
import org.dragon.skill.service.SkillUsageService;
import org.dragon.tools.AgentTool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SkillTool — Agent 可调用的 Skill 工具，实现 AgentTool 接口。
 *
 * <p>在 Agent 框架的 Tool 体系中，SkillTool 与 ExecTool/FileTools 等处于同一层，
 * 模型通过 tool_use 调用，input schema 为：
 * <pre>
 * {
 *   "skill": "deploy-check",   // Skill 名称（必填）
 *   "args":  "staging"         // 传给 Skill 的参数（可选）
 * }
 * </pre>
 *
 * <p>执行链路：
 * <pre>
 * 模型 tool_use
 *   │
 *   ▼
 * SkillTool.execute(ToolContext)
 *   │
 *   ├─▶ SkillRegistry.getSkills(agentContext)     — 多来源聚合（含缓存）
 *   │
 *   ├─▶ SkillFilter.filter(skills, agentContext)  — 动态可见性过滤
 *   │
 *   ├─▶ 校验：skill 存在 + 非 disableModelInvocation
 *   │
 *   ├─▶ SkillExecutor.execute() inline/fork 分支
 *   │
 *   └─▶ SkillToolData.persistContent（留存内容）
 *
 * 返回 ToolResult.ok(output, SkillToolData)
 * </pre>
 */
@Slf4j
@Component
public class SkillTool implements AgentTool {

    public static final String TOOL_NAME = "Skill";
    public static final String TOOL_DESCRIPTION =
            "Execute a skill by name. Skills are predefined prompts that guide the agent "
            + "to perform specific tasks following established patterns and constraints.";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private SkillRegistry          skillRegistry;
    @Autowired private SkillFilter            skillFilter;
    @Autowired private SkillExecutor          skillExecutor;
    @Autowired private SkillPermissionChecker permissionChecker;
    @Autowired private SkillUsageService      usageService;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    @Override
    public JsonNode getParameterSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = schema.putObject("properties");
        ObjectNode skill = properties.putObject("skill");
        skill.put("type", "string");
        skill.put("description", "The skill name to execute. E.g., \"deploy-check\", \"git-commit\"");

        ObjectNode args = properties.putObject("args");
        args.put("type", "string");
        args.put("description", "Optional arguments to pass to the skill");

        schema.putArray("required").add("skill");
        return schema;
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolContext context) {
        return CompletableFuture.supplyAsync(() -> doExecute(context));
    }

    private ToolResult doExecute(ToolContext context) {
        JsonNode params = context.getParameters();
        String skillName = extractSkillName(params);
        String args = extractArgs(params);

        if (!StringUtils.hasText(skillName)) {
            return ToolResult.fail("skill 参数不能为空");
        }

        // 从 ToolContext 构建 AgentContext
        AgentContext agentContext = buildAgentContext(context);

        // 1. 加载聚合 Skill 列表（多来源，含缓存）
        List<SkillDefinition> skills = skillRegistry.getSkills(context.getCharacterId(), context.getWorkspaceId());

        // 2. 动态可见性过滤
        List<SkillDefinition> visible = skillFilter.filter(skills);

        // 3. 查找目标 Skill
        SkillDefinition skill = findByName(visible, skillName);
        if (skill == null) {
            return ToolResult.fail("未找到 Skill: " + skillName
                    + "（可能未激活、已禁用，或在当前路径上下文中不可见）");
        }

        if (skill.isDisableModelInvocation()) {
            return ToolResult.fail("Skill [" + skillName + "] 已禁用模型自动调用");
        }

        // 4. 权限检查
        SkillPermissionResult permResult = permissionChecker.check(skill, agentContext);
        if (!permResult.isAllow()) {
            String reason = permResult.isDeny()
                    ? "Skill [" + skillName + "] 被权限规则拒绝"
                    : "Skill [" + skillName + "] 需要用户授权才能执行";
            log.warn("[SkillTool] 权限校验失败: name={}, behavior={}, rule={}",
                    skillName, permResult.getBehavior(), permResult.getMatchedRule());
            return ToolResult.fail(reason);
        }

        // 5. 执行
        long startMs = System.currentTimeMillis();
        try {
            SkillToolData skillData = skillExecutor.execute(
                    skill, args, context.getSessionKey(), agentContext);

            // 6. 记录使用（成功）
            usageService.recordSuccess(skill, agentContext, context.getSessionKey(), args,
                    System.currentTimeMillis() - startMs);

            // 7. 构建输出描述
            String output = buildOutputDescription(skill, skillData);

            log.info("[SkillTool] Skill 执行完成: name={}, mode={}",
                    skillName, skillData.getExecutionMode());

            return ToolResult.ok(output, skillData);

        } catch (Exception e) {
            usageService.recordFailure(skill, agentContext, context.getSessionKey(), args, e.getMessage());
            log.error("[SkillTool] Skill 执行失败: name={}, error={}", skillName, e.getMessage(), e);
            return ToolResult.fail("Skill [" + skillName + "] 执行失败: " + e.getMessage());
        }
    }

    /**
     * 从 ToolContext 构建 AgentContext。
     *
     * <p>直接使用 ToolContext 中框架注入的 characterId 和 workspaceId。
     */
    private AgentContext buildAgentContext(ToolContext toolContext) {
        return AgentContext.builder()
                .characterId(toolContext.getCharacterId())
                .workspaceId(toolContext.getWorkspaceId())
                .agentId(toolContext.getSessionKey())
                .build();
    }

    private String extractSkillName(JsonNode parameters) {
        if (parameters == null) return null;
        JsonNode node = parameters.get("skill");
        if (node == null || node.isNull()) return null;
        String name = node.asText().trim();
        return name.startsWith("/") ? name.substring(1) : name;
    }

    private String extractArgs(JsonNode parameters) {
        if (parameters == null) return null;
        JsonNode node = parameters.get("args");
        if (node == null || node.isNull()) return null;
        String args = node.asText().trim();
        return args.isEmpty() ? null : args;
    }

    private SkillDefinition findByName(List<SkillDefinition> skills, String name) {
        if (name == null) return null;
        return skills.stream()
                .filter(s -> name.equals(s.getName())
                        || (s.getAliases() != null && s.getAliases().contains(name)))
                .findFirst()
                .orElse(null);
    }

    private String buildOutputDescription(SkillDefinition skill, SkillToolData data) {
        if (data.getExecutionMode() == SkillToolData.ExecutionMode.FORK) {
            return "Skill [" + skill.getName() + "] is executing in a forked sub-agent. "
                    + "Waiting for result...";
        }
        return "Launching skill: " + skill.getName();
    }
}
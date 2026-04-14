package org.dragon.tool.runtime.tools;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.dragon.skill.runtime.AgentContext;
import org.dragon.skill.runtime.SkillExecutor;
import org.dragon.skill.runtime.SkillFilter;
import org.dragon.skill.runtime.SkillPermissionChecker;
import org.dragon.skill.runtime.SkillPermissionResult;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.skill.runtime.SkillDefinition;
import org.dragon.skill.runtime.SkillToolData;
import org.dragon.skill.service.SkillUsageService;
import org.dragon.tool.runtime.AbstractTool;
import org.dragon.tool.runtime.ToolProgress;
import org.dragon.tool.runtime.ToolResult;
import org.dragon.tool.runtime.ToolResultBlockParam;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.ToolUseContext;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * SKILL 类型工具（ATOMIC 内建工具）。
 *
 * <p>桥接 Skill 系统与工具平台，供 LLM 通过 tool_use 调用。
 * 输入 schema：
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
 * SkillTool.doCall(input, context, progress)
 *   │
 *   ├─▶ SkillRegistry.getSkills(characterId, workspaceId)  — 多来源聚合（含缓存）
 *   ├─▶ SkillFilter.filter(skills)                         — 动态可见性过滤
 *   ├─▶ 校验：skill 存在 + 非 disableModelInvocation
 *   ├─▶ SkillPermissionChecker.check()                     — 权限检查
 *   ├─▶ SkillExecutor.execute()                            — inline/fork 分支
 *   └─▶ SkillUsageService.recordSuccess/recordFailure      — 异步使用记录
 * </pre>
 *
 * <p>实例由 {@link org.dragon.tool.runtime.factory.SkillToolFactory} 创建，
 * 或作为 ATOMIC 内建工具直接注册到 {@link org.dragon.tool.runtime.factory.AtomicToolFactory}。
 */
@Slf4j
public class SkillTool extends AbstractTool<JsonNode, SkillToolData> {

    private final SkillRegistry skillRegistry;
    private final SkillFilter skillFilter;
    private final SkillExecutor skillExecutor;
    private final SkillPermissionChecker permissionChecker;
    private final SkillUsageService usageService;

    /**
     * 完整构造（供 SkillToolFactory 使用，绑定固定 skillName）。
     *
     * @param runtime           工具运行时快照（提供 name / description）
     * @param skillRegistry     Skill 注册中心
     * @param skillFilter       Skill 可见性过滤器
     * @param skillExecutor     Skill 执行器
     * @param permissionChecker Skill 权限检查器
     * @param usageService      Skill 使用记录服务
     */
    public SkillTool(ToolDefinition runtime,
                     SkillRegistry skillRegistry,
                     SkillFilter skillFilter,
                     SkillExecutor skillExecutor,
                     SkillPermissionChecker permissionChecker,
                     SkillUsageService usageService) {
        super(runtime.getName(), runtime.getDescription(), JsonNode.class);
        this.skillRegistry = skillRegistry;
        this.skillFilter = skillFilter;
        this.skillExecutor = skillExecutor;
        this.permissionChecker = permissionChecker;
        this.usageService = usageService;
    }

    @Override
    protected CompletableFuture<ToolResult<SkillToolData>> doCall(JsonNode input,
                                                                  ToolUseContext context,
                                                                  Consumer<ToolProgress> progress) {
        return CompletableFuture.supplyAsync(() -> {
            // 1. 解析目标 Skill 名称（input.skill 优先，fallback 到 boundSkillName）
            String skillName = extractSkillName(input);
            if (!StringUtils.hasText(skillName)) {
                return ToolResult.fail("SkillTool: skill 参数不能为空");
            }

            String args = extractArgs(input);
            String characterId = context.getCharacterId();
            String workspaceId = context.getWorkspaceId();
            String sessionKey = context.getSessionId();

            // 2. 构建 AgentContext
            AgentContext agentContext = AgentContext.builder()
                    .characterId(characterId)
                    .workspaceId(workspaceId)
                    .agentId(context.getAgentId())
                    .build();

            // 3. 加载聚合 Skill 列表（多来源，含缓存）
            List<SkillDefinition> skills = skillRegistry.getSkills(characterId, workspaceId);

            // 4. 动态可见性过滤
            List<SkillDefinition> visible = skillFilter.filter(skills);

            // 5. 查找目标 Skill
            SkillDefinition skill = findByName(visible, skillName);
            if (skill == null) {
                return ToolResult.fail("SkillTool: 未找到 Skill: " + skillName
                        + "（可能未激活、已禁用，或在当前上下文中不可见）。可用列表: "
                        + visible.stream().map(SkillDefinition::getName).collect(Collectors.joining(", ")));
            }

            if (skill.isDisableModelInvocation()) {
                return ToolResult.fail("SkillTool: Skill [" + skillName + "] 已禁用模型自动调用");
            }

            // 6. 权限检查
            SkillPermissionResult permResult = permissionChecker.check(skill, agentContext);
            if (!permResult.isAllow()) {
                String reason = permResult.isDeny()
                        ? "SkillTool: Skill [" + skillName + "] 被权限规则拒绝"
                        : "SkillTool: Skill [" + skillName + "] 需要用户授权才能执行";
                log.warn("[SkillTool] 权限校验失败: name={}, behavior={}, rule={}",
                        skillName, permResult.getBehavior(), permResult.getMatchedRule());
                return ToolResult.fail(reason);
            }

            // 7. 执行
            long startMs = System.currentTimeMillis();
            try {
                SkillToolData skillData = skillExecutor.execute(skill, args, sessionKey, agentContext);

                // 8. 异步记录使用（成功）
                usageService.recordSuccess(skill, agentContext, sessionKey, args,
                        System.currentTimeMillis() - startMs);

                log.info("[SkillTool] Skill 执行完成: name={}, mode={}",
                        skillName, skillData.getExecutionMode());

                // 9. 返回结果；newMessages 由框架层从 SkillToolData.getNewMessages() 读取后注入对话
                return ToolResult.ok(skillData);

            } catch (Exception e) {
                usageService.recordFailure(skill, agentContext, sessionKey, args, e.getMessage());
                log.error("[SkillTool] Skill 执行失败: name={}, error={}", skillName, e.getMessage(), e);
                return ToolResult.fail("SkillTool: Skill [" + skillName + "] 执行失败: " + e.getMessage());
            }
        });
    }

    @Override
    public ToolResultBlockParam mapToolResultToToolResultBlockParam(SkillToolData output, String toolUseId) {
        // Skill 是内容注入型工具：tool_result 只需告知 LLM skill 已激活，
        // 实际 prompt 通过 ToolResult.newMessages 由框架主循环注入对话上下文
        if (output == null) {
            return ToolResultBlockParam.ofText(toolUseId, "Skill has been activated.");
        }
        String ackText = output.getExecutionMode() == SkillToolData.ExecutionMode.FORK
                ? "Skill [" + output.getSkillName() + "] is executing in a forked sub-agent. Waiting for result..."
                : "Launching skill: " + output.getSkillName();
        return ToolResultBlockParam.ofText(toolUseId, ackText);
    }

    @Override
    public boolean isReadOnly(JsonNode input) {
        return true;
    }

    // ── 内部辅助 ────────────────────────────────────────────────────

    private String extractSkillName(JsonNode params) {
        if (params == null) return null;
        JsonNode node = params.get("skill");
        if (node == null || node.isNull()) return null;
        String name = node.asText().trim();
        return name.startsWith("/") ? name.substring(1) : name;
    }

    private String extractArgs(JsonNode params) {
        if (params == null) return null;
        JsonNode node = params.get("args");
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
}
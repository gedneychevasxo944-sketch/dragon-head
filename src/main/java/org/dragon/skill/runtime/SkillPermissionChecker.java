package org.dragon.skill.runtime;

import org.dragon.config.context.InheritanceContext;
import org.dragon.config.service.ConfigApplication;
import org.dragon.skill.config.SkillPermissionConfig;
import org.dragon.skill.enums.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Skill 权限检查器（对齐 TS {@code SkillTool.checkPermissions}）。
 *
 * <h3>检查顺序（完全对齐 TS 逻辑）</h3>
 * <ol>
 *   <li><b>deny 规则</b>：命中则立即拒绝，优先级最高</li>
 *   <li><b>allow 规则</b>：命中则立即放行</li>
 *   <li><b>安全属性白名单</b>：Skill 只含安全属性则自动放行（{@link #SAFE_SKILL_PROPERTIES}）</li>
 *   <li><b>ask</b>：以上均未命中 → 根据配置策略处置：
 *       <ul>
 *         <li>{@code auto-deny}（默认）— 直接返回 {@link SkillPermissionResult#deny}</li>
 *         <li>{@code event} — 发布 {@link SkillPermissionEvent}，等待框架层授权</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h3>规则匹配规则（与 TS ruleMatches 一致）</h3>
 * <ul>
 *   <li><b>精确匹配</b>：{@code "deploy-check"} 完整等于 Skill 名称（忽略前导斜杠）</li>
 *   <li><b>前缀通配</b>：{@code "review:*"} 匹配以 {@code review} 开头的任意 Skill 名称</li>
 * </ul>
 *
 * <h3>安全属性白名单（对应 TS {@code SAFE_SKILL_PROPERTIES}）</h3>
 * 只含白名单属性的 Skill 视为"无害"，自动放行。含以下属性之一则需权限：
 * <ul>
 *   <li>{@code allowedTools}（非空列表）— 影响子 Agent 可用工具集</li>
 *   <li>{@code executionContext=fork} — 创建子 Agent，涉及资源隔离</li>
 *   <li>{@code hooks}（若未来支持）— 自定义钩子脚本</li>
 * </ul>
 */
@Slf4j
@Component
public class SkillPermissionChecker {

    /**
     * 安全属性集合（与 TS {@code SAFE_SKILL_PROPERTIES} 对齐）。
     *
     * <p>含义：Skill 的所有有意义属性均在此集合内时，可自动放行，无需用户确认。
     * 不在此集合中的属性一旦有非空/非空列表的值，即触发权限确认。
     *
     * <p>设计原则：<b>新增属性默认需要权限</b>，避免未来功能扩展带来安全漏洞。
     * 对应 TS 注释：<em>"ensures new properties added in the future default to requiring permission"</em>。
     */
    private static final Set<String> SAFE_SKILL_PROPERTIES = Set.of(
            // 标识类（无害）
            "skillId", "name", "displayName", "description", "version",
            // 可见性控制（无害）
            "whenToUse", "argumentHint", "aliases",
            "disableModelInvocation", "userInvocable",
            // 执行行为（安全子集）
            "model", "effort",
            // 内容相关（无害）
            "category", "persist", "persistMode",
            // 内部字段（技术属性）
            "storageInfo", "content"
            // 注意：以下属性不在白名单，有值时需权限：
            //   allowedTools  — 影响工具集
            //   executionContext=fork — 创建子 Agent
    );

    /**
     * ASK 事件等待超时（毫秒）。
     * 框架层未在此时间内响应，视为拒绝。
     */
    private static final long DEFAULT_ASK_TIMEOUT_MS = 30_000L;

    private final long askTimeoutMs;
    private final SkillPermissionConfig    permissionConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public SkillPermissionChecker(ConfigApplication configApplication,
                                  SkillPermissionConfig permissionConfig,
                                  ApplicationEventPublisher eventPublisher) {
        this.permissionConfig = permissionConfig;
        this.eventPublisher = eventPublisher;
        this.askTimeoutMs = configApplication.getLongValue(
                "skill.permission.ask-timeout-ms",
                InheritanceContext.forGlobal(),
                DEFAULT_ASK_TIMEOUT_MS
        );
    }

    // ── 对外接口 ─────────────────────────────────────────────────────────

    /**
     * 执行完整权限检查，返回决策结果。
     *
     * <p>调用方（{@link SkillTool}）应在确认 {@link SkillPermissionResult#isAllow()} 后再执行 Skill；
     * 若为 DENY 则直接返回错误；若为 ASK 且策略为 {@code event}，则等待框架层异步授权。
     *
     * @param skill        待执行的 Skill 运行时定义
     * @param agentContext 当前 Agent 上下文
     * @return 权限决策结果
     */
    public SkillPermissionResult check(SkillDefinition skill, AgentContext agentContext) {
        String skillName = skill.getName();

        // 1. 检查 deny 规则（优先级最高）
        String denyMatch = findMatchingRule(permissionConfig.getDenyRules(), skillName);
        if (denyMatch != null) {
            log.info("[SkillPermission] DENY: skill='{}', rule='{}'", skillName, denyMatch);
            return SkillPermissionResult.deny(denyMatch);
        }

        // 2. 检查 allow 规则
        String allowMatch = findMatchingRule(permissionConfig.getAllowRules(), skillName);
        if (allowMatch != null) {
            log.debug("[SkillPermission] ALLOW (rule): skill='{}', rule='{}'", skillName, allowMatch);
            return SkillPermissionResult.allow(allowMatch);
        }

        // 3. 安全属性白名单自动放行
        if (hasOnlySafeProperties(skill)) {
            log.debug("[SkillPermission] ALLOW (safe-properties): skill='{}'", skillName);
            return SkillPermissionResult.allow(null);
        }

        // 4. 无规则命中 & 含非安全属性 → ASK
        List<SkillPermissionResult.Suggestion> suggestions = buildSuggestions(skillName);
        log.info("[SkillPermission] ASK: skill='{}', strategy='{}'",
                skillName, permissionConfig.getAskStrategy());

        if (permissionConfig.isEventAskStrategy()) {
            return handleAskWithEvent(skill, agentContext, suggestions);
        }

        // auto-deny 策略（默认）：直接拒绝并说明原因
        return SkillPermissionResult.deny("__ask_auto_deny__");
    }

    // ── 规则匹配（对齐 TS ruleMatches） ─────────────────────────────────

    /**
     * 在规则列表中查找第一个命中的规则，返回规则内容；无命中则返回 null。
     *
     * <p>匹配逻辑与 TS {@code ruleMatches(ruleContent)} 完全一致：
     * <ol>
     *   <li>精确匹配：规则内容（去掉前导 /）与 skillName 完整相等</li>
     *   <li>前缀通配：规则内容以 {@code :*} 结尾，skillName 以规则前缀开头</li>
     * </ol>
     */
    private String findMatchingRule(List<String> rules, String skillName) {
        if (CollectionUtils.isEmpty(rules)) return null;

        for (String rule : rules) {
            if (!StringUtils.hasText(rule)) continue;

            // 规范化规则：去掉前导斜杠（与 TS normalizedRule 一致）
            String normalizedRule = rule.startsWith("/") ? rule.substring(1) : rule;

            // 精确匹配
            if (normalizedRule.equals(skillName)) {
                return rule;
            }

            // 前缀通配：review:* → skillName.startsWith("review")
            if (normalizedRule.endsWith(":*")) {
                String prefix = normalizedRule.substring(0, normalizedRule.length() - 2);
                if (skillName.startsWith(prefix)) {
                    return rule;
                }
            }
        }
        return null;
    }

    // ── 安全属性白名单（对齐 TS skillHasOnlySafeProperties） ─────────────

    /**
     * 检查 Skill 是否只含安全属性。
     *
     * <p>对应 TS {@code skillHasOnlySafeProperties(command)}：
     * 遍历 Skill 属性，若存在不在白名单中且有意义值（非 null / 非空列表）的属性，则返回 false。
     *
     * <p>Java 实现采用字段逐一检查方式（而非反射），保持代码可读性和类型安全：
     * <ul>
     *   <li>{@code allowedTools} — 非空列表时需权限</li>
     *   <li>{@code executionContext=fork} — fork 模式需权限</li>
     * </ul>
     */
    private boolean hasOnlySafeProperties(SkillDefinition skill) {
        // allowedTools 非空 → 不安全（影响子 Agent 可用工具集）
        if (!CollectionUtils.isEmpty(skill.getAllowedTools())) {
            log.debug("[SkillPermission] 非安全属性: skill='{}', reason=allowedTools非空",
                    skill.getName());
            return false;
        }

        // executionContext=fork → 不安全（创建子 Agent，涉及资源隔离）
        if (ExecutionContext.FORK == skill.getExecutionContext()) {
            log.debug("[SkillPermission] 非安全属性: skill='{}', reason=executionContext=fork",
                    skill.getName());
            return false;
        }

        // 其余属性均在白名单内，视为安全
        return true;
    }

    // ── ASK 事件策略 ─────────────────────────────────────────────────────

    /**
     * 事件策略处理：发布 {@link SkillPermissionEvent}，等待框架层异步授权。
     *
     * <p>若框架层在 {@value #DEFAULT_ASK_TIMEOUT_MS}ms 内未响应，视为拒绝。
     */
    private SkillPermissionResult handleAskWithEvent(SkillDefinition skill,
                                                      AgentContext agentContext,
                                                      List<SkillPermissionResult.Suggestion> suggestions) {
        SkillPermissionEvent event = new SkillPermissionEvent(
                this, skill.getName(), agentContext, suggestions);

        eventPublisher.publishEvent(event);

        boolean authorized = event.awaitAuthorization(askTimeoutMs);
        if (authorized) {
            log.info("[SkillPermission] ALLOW (ask-event authorized): skill='{}'", skill.getName());
            return SkillPermissionResult.allow(null);
        } else {
            log.info("[SkillPermission] DENY (ask-event rejected/timeout): skill='{}'", skill.getName());
            return SkillPermissionResult.deny("__ask_rejected__");
        }
    }

    // ── Suggestions 构建（对应 TS suggestions 数组） ────────────────────

    /**
     * 构建 ASK 时的建议规则列表（对应 TS {@code suggestions}）。
     *
     * <p>始终包含两条（与 TS 一致）：
     * <ol>
     *   <li>精确规则：{@code "skill-name"} — 只允许此 Skill</li>
     *   <li>前缀规则：{@code "skill-name:*"} — 允许此 Skill 带任意参数执行</li>
     * </ol>
     */
    private List<SkillPermissionResult.Suggestion> buildSuggestions(String skillName) {
        return List.of(
                new SkillPermissionResult.Suggestion(skillName,         "allow"),
                new SkillPermissionResult.Suggestion(skillName + ":*",  "allow")
        );
    }
}


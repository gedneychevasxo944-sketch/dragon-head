package org.dragon.skill.runtime;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Skill 权限检查结果（对齐 TS {@code PermissionDecision}）。
 *
 * <p>TS 版本三种行为：
 * <pre>
 * behavior: 'allow' → 明确放行（规则命中 / 只含安全属性）
 * behavior: 'deny'  → 明确拒绝（deny 规则命中）
 * behavior: 'ask'   → 弹出确认（无规则命中 & Skill 含非安全属性）
 * </pre>
 *
 * <p>Java 对应：
 * <ul>
 *   <li>{@link Behavior#ALLOW} — 直接通过</li>
 *   <li>{@link Behavior#DENY}  — 直接拒绝，{@code message} 说明原因</li>
 *   <li>{@link Behavior#ASK}   — 发布 {@link SkillPermissionEvent}，
 *       附带 {@link Suggestion} 列表供框架层展示给用户</li>
 * </ul>
 */
@Getter
public class SkillPermissionResult {

    /** 权限判定行为 */
    public enum Behavior {
        /** 明确放行 */
        ALLOW,
        /** 明确拒绝 */
        DENY,
        /**
         * 需要用户确认（发布事件）。
         * Java 服务端 Agent 收到此结果后，应发布 {@link SkillPermissionEvent}，
         * 由框架层决定阻塞等待用户确认，还是按默认策略（如默认拒绝）处理。
         */
        ASK
    }

    /**
     * 权限确认建议（对应 TS suggestions）。
     *
     * <p>ASK 时返回两条建议：
     * <ol>
     *   <li>精确规则：{@code skill-name}（只允许此 Skill 名称）</li>
     *   <li>前缀规则：{@code skill-name:*}（允许该 Skill 带任意参数执行）</li>
     * </ol>
     */
    public record Suggestion(
            /** 规则内容，如 "deploy-check" 或 "review:*" */
            String ruleContent,
            /** 建议行为（通常是 allow） */
            String behavior
    ) {}

    // ── 字段 ────────────────────────────────────────────────────────────

    private final Behavior behavior;

    /**
     * 拒绝 / 确认时的说明信息（deny/ask 时非空）。
     */
    private final String message;

    /**
     * 触发决策的规则内容（规则命中时非空，安全属性自动放行时为 null）。
     * 对应 TS decisionReason.rule.ruleContent。
     */
    private final String matchedRule;

    /**
     * ASK 时附带的建议规则列表（对应 TS suggestions）。
     */
    private final List<Suggestion> suggestions;

    // ── 私有构造 ────────────────────────────────────────────────────────

    private SkillPermissionResult(Behavior behavior, String message,
                                   String matchedRule, List<Suggestion> suggestions) {
        this.behavior    = behavior;
        this.message     = message;
        this.matchedRule = matchedRule;
        this.suggestions = suggestions != null ? Collections.unmodifiableList(suggestions)
                                               : Collections.emptyList();
    }

    // ── 工厂方法（对应 TS 三种返回值） ────────────────────────────────────

    /**
     * 明确放行（规则命中 allow，或只含安全属性自动放行）。
     *
     * @param matchedRule 命中的规则内容，若安全属性自动放行则传 null
     */
    public static SkillPermissionResult allow(String matchedRule) {
        return new SkillPermissionResult(Behavior.ALLOW, null, matchedRule, null);
    }

    /**
     * 明确拒绝（deny 规则命中）。
     *
     * @param matchedRule 命中的 deny 规则内容
     */
    public static SkillPermissionResult deny(String matchedRule) {
        return new SkillPermissionResult(
                Behavior.DENY,
                "Skill execution blocked by permission rules",
                matchedRule,
                null
        );
    }

    /**
     * 需要用户确认（无规则命中 & 含非安全属性），附带 suggestions。
     *
     * @param skillName   Skill 名称（用于构造 message 和 suggestions）
     * @param suggestions 推荐规则列表（精确 + 前缀）
     */
    public static SkillPermissionResult ask(String skillName, List<Suggestion> suggestions) {
        return new SkillPermissionResult(
                Behavior.ASK,
                "Execute skill: " + skillName,
                null,
                suggestions
        );
    }

    // ── 便捷判断方法 ─────────────────────────────────────────────────────

    public boolean isAllow() { return behavior == Behavior.ALLOW; }
    public boolean isDeny()  { return behavior == Behavior.DENY;  }
    public boolean isAsk()   { return behavior == Behavior.ASK;   }
}


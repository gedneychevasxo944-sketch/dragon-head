package org.dragon.tool.runtime;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 权限检查结果。
 *
 * <p>对应 TypeScript 版本的权限相关类型。
 * 权限检查流程（对应 TS checkPermissions）：
 * <ol>
 *   <li>DENY：命中拒绝规则 → 直接拒绝</li>
 *   <li>ALLOW：命中允许规则 → 直接允许</li>
 *   <li>安全属性白名单：检查工具属性是否在白名单内</li>
 *   <li>ASK：需要用户确认</li>
 * </ol>
 */
@Data
@Builder
public class PermissionResult {

    /**
     * 权限行为。
     */
    public enum Behavior {
        /**
         * 允许执行。
         */
        ALLOW,
        /**
         * 拒绝执行。
         */
        DENY,
        /**
         * 需要用户确认。
         */
        ASK
    }

    /**
     * 权限行为。
     */
    private final Behavior behavior;

    /**
     * 结果消息。
     */
    private final String message;

    /**
     * 匹配的规则名称。
     */
    private final String matchedRule;

    /**
     * 规则来源。
     */
    private final String ruleSource;

    /**
     * 用户修改后的输入（当用户在权限确认时修改输入）。
     */
    private final Map<String, Object> updatedInput;

    /**
     * 用户反馈（允许时）。
     */
    private final String acceptFeedback;

    /**
     * 额外的内容块（如用户粘贴的图片）。
     */
    private final List<Object> contentBlocks;

    /**
     * 决策原因。
     */
    private final DecisionReason decisionReason;

    // ── 静态工厂方法 ─────────────────────────────────────────────────────

    /**
     * 创建允许结果。
     */
    public static PermissionResult allow() {
        return PermissionResult.builder()
                .behavior(Behavior.ALLOW)
                .build();
    }

    /**
     * 创建允许结果（带规则信息）。
     */
    public static PermissionResult allow(String matchedRule, String ruleSource) {
        return PermissionResult.builder()
                .behavior(Behavior.ALLOW)
                .matchedRule(matchedRule)
                .ruleSource(ruleSource)
                .build();
    }

    /**
     * 创建拒绝结果。
     */
    public static PermissionResult deny(String message) {
        return PermissionResult.builder()
                .behavior(Behavior.DENY)
                .message(message)
                .build();
    }

    /**
     * 创建拒绝结果（带规则信息）。
     */
    public static PermissionResult deny(String message, String matchedRule, String ruleSource) {
        return PermissionResult.builder()
                .behavior(Behavior.DENY)
                .message(message)
                .matchedRule(matchedRule)
                .ruleSource(ruleSource)
                .build();
    }

    /**
     * 创建需要确认的结果。
     */
    public static PermissionResult ask(String message) {
        return PermissionResult.builder()
                .behavior(Behavior.ASK)
                .message(message)
                .build();
    }

    /**
     * 创建需要确认的结果（带建议规则）。
     */
    public static PermissionResult ask(String message, List<String> suggestedRules) {
        return PermissionResult.builder()
                .behavior(Behavior.ASK)
                .message(message)
                .decisionReason(new DecisionReason("permissionPromptTool", suggestedRules))
                .build();
    }

    // ── 便捷方法 ─────────────────────────────────────────────────────────

    public boolean isAllow() {
        return behavior == Behavior.ALLOW;
    }

    public boolean isDeny() {
        return behavior == Behavior.DENY;
    }

    public boolean isAsk() {
        return behavior == Behavior.ASK;
    }

    // ── 内部类型 ─────────────────────────────────────────────────────────

    /**
     * 决策原因。
     */
    @Data
    public static class DecisionReason {
        private final String type;
        private final List<String> suggestedRules;
        private final String ruleSource;
        private final String hookName;

        public DecisionReason(String type) {
            this.type = type;
            this.suggestedRules = null;
            this.ruleSource = null;
            this.hookName = null;
        }

        public DecisionReason(String type, List<String> suggestedRules) {
            this.type = type;
            this.suggestedRules = suggestedRules;
            this.ruleSource = null;
            this.hookName = null;
        }
    }
}

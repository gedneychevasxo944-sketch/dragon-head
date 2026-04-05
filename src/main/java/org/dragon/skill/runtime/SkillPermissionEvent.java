package org.dragon.skill.runtime;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Skill 权限确认事件（对应 TS {@code behavior: 'ask'} + suggestions）。
 *
 * <p>当 {@link SkillPermissionChecker} 无法通过规则或安全属性白名单自动放行时，
 * 若配置 {@code skill.permission.ask-strategy=event}，则发布此事件，
 * 由框架层（UI 交互层）接收并决定是否授权。
 *
 * <p>框架层处理示例：
 * <pre>
 * {@code
 * @EventListener
 * public void onPermissionAsk(SkillPermissionEvent event) {
 *     // 展示确认弹窗，用户点击确认/拒绝后调用：
 *     event.complete(true);   // 授权
 *     event.complete(false);  // 拒绝
 * }
 * }
 * </pre>
 *
 * <p>若框架层不处理（超时或无监听），{@link SkillPermissionChecker} 默认视为拒绝。
 */
@Getter
public class SkillPermissionEvent extends ApplicationEvent {

    /** 请求执行的 Skill 名称 */
    private final String skillName;

    /** 执行者的 Agent 上下文 */
    private final AgentContext agentContext;

    /**
     * 推荐规则建议（对应 TS suggestions）。
     * 通常包含两条：精确规则 + 前缀规则。
     */
    private final List<SkillPermissionResult.Suggestion> suggestions;

    /**
     * 异步授权回调（框架层调用 {@link #complete(boolean)} 后 SkillPermissionChecker 可继续执行）。
     */
    private final CompletableFuture<Boolean> authorizationFuture = new CompletableFuture<>();

    // ── 构造 ─────────────────────────────────────────────────────────────

    public SkillPermissionEvent(Object source,
                                 String skillName,
                                 AgentContext agentContext,
                                 List<SkillPermissionResult.Suggestion> suggestions) {
        super(source);
        this.skillName    = skillName;
        this.agentContext = agentContext;
        this.suggestions  = suggestions;
    }

    // ── 框架层调用 ───────────────────────────────────────────────────────

    /**
     * 框架层（UI 交互层）在用户确认或拒绝后调用此方法。
     *
     * @param authorized true = 用户授权；false = 用户拒绝
     */
    public void complete(boolean authorized) {
        authorizationFuture.complete(authorized);
    }

    /**
     * SkillPermissionChecker 内部使用：等待框架层的授权结果。
     *
     * @param timeoutMs 等待超时毫秒数，超时视为拒绝
     * @return 是否授权
     */
    public boolean awaitAuthorization(long timeoutMs) {
        try {
            return authorizationFuture.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // 超时 / 中断 / 异常均视为拒绝
            return false;
        }
    }
}


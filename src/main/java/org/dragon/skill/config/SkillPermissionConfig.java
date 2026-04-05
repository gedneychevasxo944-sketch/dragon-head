package org.dragon.skill.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill 权限控制配置（对齐 TS {@code SkillTool.checkPermissions} 机制）。
 *
 * <p>从 {@code application.yml} 的 {@code skill.permission} 节读取，示例：
 * <pre>
 * skill:
 *   permission:
 *     deny-rules:
 *       - "deploy-prod"   # 精确拒绝
 *       - "deploy:*"      # 前缀通配拒绝
 *     allow-rules:
 *       - "git-commit"
 *     ask-strategy: auto-deny   # auto-deny | event
 * </pre>
 *
 * <h3>规则格式（与 TS 保持一致）</h3>
 * <ul>
 *   <li><b>精确匹配</b>：{@code "deploy-prod"} — 完整 Skill 名称相等</li>
 *   <li><b>前缀通配</b>：{@code "review:*"} — 以 {@code review} 开头的任意 Skill 名称</li>
 * </ul>
 *
 * <h3>ASK 策略</h3>
 * <ul>
 *   <li>{@code auto-deny} — 无规则命中 & 含非安全属性时，直接拒绝（默认，适合无交互 Agent 环境）</li>
 *   <li>{@code event}     — 发布 {@link org.dragon.skill.runtime.SkillPermissionEvent}，
 *       由框架层决定是否继续</li>
 * </ul>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "skill.permission")
public class SkillPermissionConfig {

    /**
     * deny 规则列表（优先级最高，命中则拒绝）。
     * 默认为空列表。
     */
    private List<String> denyRules = new ArrayList<>();

    /**
     * allow 规则列表（deny 未命中后检查，命中则放行）。
     * 默认为空列表。
     */
    private List<String> allowRules = new ArrayList<>();

    /**
     * ASK 处置策略。
     *
     * <ul>
     *   <li>{@code auto-deny} — 默认，直接拒绝并返回错误（适合 CI/CD 或无 UI 环境）</li>
     *   <li>{@code event}     — 发布 {@code SkillPermissionEvent}，由框架层接收后处理</li>
     * </ul>
     */
    private String askStrategy = "auto-deny";

    // ── 便捷方法 ─────────────────────────────────────────────────────────

    /**
     * 是否使用事件驱动 ASK 策略。
     */
    public boolean isEventAskStrategy() {
        return "event".equalsIgnoreCase(askStrategy);
    }
}


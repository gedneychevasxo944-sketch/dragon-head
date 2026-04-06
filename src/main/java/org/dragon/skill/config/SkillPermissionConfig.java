package org.dragon.skill.config;

import org.dragon.config.context.InheritanceContext;
import org.dragon.config.service.ConfigApplication;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Skill 权限控制配置（对齐 TS {@code SkillTool.checkPermissions} 机制）。
 *
 * <p>从 ConfigStore 读取配置，示例：
 * <pre>
 * skill.permission.deny-rules:
 *   - "deploy-prod"   # 精确拒绝
 *   - "deploy:*"      # 前缀通配拒绝
 * skill.permission.allow-rules:
 *   - "git-commit"
 * skill.permission.ask-strategy: auto-deny   # auto-deny | event
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
@Component
public class SkillPermissionConfig {

    private List<String> denyRules = new ArrayList<>();
    private List<String> allowRules = new ArrayList<>();
    private String askStrategy = "auto-deny";

    @Autowired
    public SkillPermissionConfig(ConfigApplication configApplication) {
        InheritanceContext ctx = InheritanceContext.forGlobal();
        this.denyRules = configApplication.getListValue("skill.permission.deny-rules", ctx, new ArrayList<>());
        this.allowRules = configApplication.getListValue("skill.permission.allow-rules", ctx, new ArrayList<>());
        this.askStrategy = configApplication.getStringValue("skill.permission.ask-strategy", ctx, "auto-deny");
    }

    /**
     * 是否使用事件驱动 ASK 策略。
     */
    public boolean isEventAskStrategy() {
        return "event".equalsIgnoreCase(askStrategy);
    }
}


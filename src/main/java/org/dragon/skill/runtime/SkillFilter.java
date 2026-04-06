package org.dragon.skill.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Skill 可见性过滤器（设计点 3 - 动态可见性）。
 *
 * <p>在每次对话开始时，对 SkillRegistry 返回的聚合列表做两层过滤：
 * <ol>
 *   <li><b>disableModelInvocation 过滤</b>：排除标记为不可模型调用的 Skill</li>
 *   <li><b>isEnabled 动态判断</b>：预留扩展点（feature flag、时间窗口等）</li>
 * </ol>
 *
 * <p>注意：status=active 的保证已由 SkillRegistry.loadFromDB() 在加载时确保，
 * 此处不再重复过滤，避免冗余。
 */
@Slf4j
@Component
public class SkillFilter {

    /**
     * 对聚合列表做过滤，返回本次对话中可见的 Skill 列表。
     *
     * @param skills       SkillRegistry 返回的聚合列表
     * @param agentContext 当前 Agent 上下文
     * @return 过滤后的可见 Skill 列表
     */
    public List<SkillRuntime> filter(List<SkillRuntime> skills) {
        return skills.stream()
                .filter(s -> !s.isDisableModelInvocation())
                .filter(this::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 动态可见性判断（isEnabled 扩展点）。
     *
     * <p>当前实现始终返回 true，预留扩展：
     * <ul>
     *   <li>Feature flag 开关（如 GrowthBook 实验）</li>
     *   <li>时间窗口（仅在特定时间段内可见）</li>
     *   <li>A/B 测试灰度</li>
     * </ul>
     */
    protected boolean isEnabled(SkillRuntime skill) {
        // 扩展点：可注入 FeatureFlagService 等实现动态控制
        return true;
    }
}


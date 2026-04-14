package org.dragon.tool.runtime.factory;

import com.fasterxml.jackson.databind.JsonNode;
import org.dragon.skill.runtime.SkillExecutor;
import org.dragon.skill.runtime.SkillFilter;
import org.dragon.skill.runtime.SkillPermissionChecker;
import org.dragon.skill.runtime.SkillRegistry;
import org.dragon.skill.service.SkillUsageService;
import org.dragon.tool.enums.ToolType;
import org.dragon.tool.runtime.Tool;
import org.dragon.tool.runtime.ToolFactory;
import org.dragon.tool.runtime.ToolDefinition;
import org.dragon.tool.runtime.tools.SkillTool;

import java.util.Objects;

/**
 * SKILL 类型工具 Factory。
 *
 * <p>持有 Skill 系统的基础设施依赖（{@link SkillRegistry} / {@link SkillFilter} /
 * {@link SkillExecutor} / {@link SkillPermissionChecker} / {@link SkillUsageService}），
 * 构建绑定了具体 skillName 的 {@link SkillTool} 实例。
 *
 * <p>实例可安全跨调用复用（{@link #isSingleton()} 返回 {@code true}）。
 */
public class SkillToolFactory implements ToolFactory {

    private final SkillRegistry skillRegistry;
    private final SkillFilter skillFilter;
    private final SkillExecutor skillExecutor;
    private final SkillPermissionChecker permissionChecker;
    private final SkillUsageService usageService;

    public SkillToolFactory(SkillRegistry skillRegistry,
                            SkillFilter skillFilter,
                            SkillExecutor skillExecutor,
                            SkillPermissionChecker permissionChecker,
                            SkillUsageService usageService) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry must not be null");
        this.skillFilter = Objects.requireNonNull(skillFilter, "skillFilter must not be null");
        this.skillExecutor = Objects.requireNonNull(skillExecutor, "skillExecutor must not be null");
        this.permissionChecker = Objects.requireNonNull(permissionChecker, "permissionChecker must not be null");
        this.usageService = Objects.requireNonNull(usageService, "usageService must not be null");
    }

    @Override
    public ToolType supportedType() {
        return ToolType.SKILL;
    }

    /**
     * 构建绑定了 skillName 的 {@link SkillTool} 实例。
     *
     * <p>skillName 来自 {@code executionConfig.skillName}，可为空（此时 LLM 通过
     * rawParams.skill 动态传入 Skill 名称）。
     */
    @Override
    public Tool<JsonNode, ?> create(ToolDefinition runtime) {
        JsonNode config = runtime.getExecutionConfig();
        // skillName 可为空（LLM 动态指定），此时 SkillTool 从 rawParams.skill 中读取
        String skillName = (config != null) ? config.path("skillName").asText(null) : null;

        return new SkillTool(runtime, skillRegistry, skillFilter, skillExecutor,
                permissionChecker, usageService, skillName);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}


package org.dragon.skill.runtime;

import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillEffort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Skill 执行器（设计点 4 + 5 + 6）。
 *
 * <p>职责：
 * <ul>
 *   <li><b>inline 模式（设计点 4）</b>：在当前对话上下文中展开完整 prompt，
 *       构建注入消息列表（newMessages）返回给框架层</li>
 *   <li><b>fork 模式（设计点 4）</b>：创建独立 sub-AgentTask 执行，
 *       父 Agent 等待结果返回。具体 Task 创建由框架的 TaskService 负责，
 *       此处只组装执行参数并委托。</li>
 *   <li><b>allowedTools 注入（设计点 5）</b>：构建 ContextPatch，声明工具权限变更，
 *       由框架层应用，执行结束后自动恢复</li>
 *   <li><b>persist 处理（设计点 6）</b>：执行后判断 Skill 的 persist 字段，
 *       提取需要持续留存的内容，写入 SkillToolData.persistContent，
 *       框架层再调用 session.addPersistedSkill()</li>
 * </ul>
 */
@Slf4j
@Component
public class SkillExecutor {

    /**
     * 本次对话生命周期内的 content 缓存：skillId → 已加载的 prompt 正文。
     * 避免同一对话内多次调用同一 Skill 重复获取。
     * Key 用 sessionKey + ":" + skillId 隔离不同对话。
     */
    private final Map<String, String> contentCache = new ConcurrentHashMap<>();

    private final SkillWorkspaceManager workspaceManager;

    public SkillExecutor(SkillWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    /**
     * 执行 Skill，返回结构化的执行结果。
     *
     * <p>框架层根据 {@link SkillToolData#getExecutionMode()} 决定后续处理：
     * <ul>
     *   <li>INLINE：把 newMessages 注入当前对话，应用 contextPatch</li>
     *   <li>FORK：创建 sub-AgentTask 执行，等待结果</li>
     * </ul>
     *
     * @param skill      要执行的 Skill 定义
     * @param args       用户传入的参数（可为 null）
     * @param sessionKey 当前会话 key（用于 content 缓存隔离）
     * @param agentContext 当前 Agent 上下文（fork 时用于追踪父子关系）
     * @return SkillToolData（由框架层解析并应用）
     */
    public SkillToolData execute(SkillDefinition skill, String args,
                                 String sessionKey, AgentContext agentContext) {
        log.info("[SkillExecutor] 执行 Skill: name={}, mode={}, sessionKey={}",
                skill.getName(), skill.getExecutionContext(), sessionKey);

        // 判断是否需要物化工作目录（Skill 包含附属文件时才需要）
        if (workspaceManager.needsMaterialization(skill.getStorageInfo())) {
            return executeWithWorkspace(skill, args, sessionKey, agentContext);
        } else {
            return executeWithContentOnly(skill, args, sessionKey, agentContext);
        }
    }

    /**
     * 含附属文件的执行路径：物化执行工作目录，执行完毕后清理。
     * 工作目录路径通过 ContextPatch.skillWorkDir 传递给框架层。
     */
    private SkillToolData executeWithWorkspace(SkillDefinition skill, String args,
                                               String sessionKey, AgentContext agentContext) {
        Path execDir = workspaceManager.prepareExecDir(
                skill.getSkillId(), skill.getVersion(), skill.getStorageInfo());
        try {
            // 直接从工作目录读取 SKILL.md（已物化到本地）
            String content = readSkillMdFromDir(execDir);
            String processedContent = injectArgs(content, args);
            ContextPatch contextPatch = buildContextPatch(skill, execDir.toString());
            String persistContent = resolvePersistContent(skill, processedContent);

            log.info("[SkillExecutor] 工作区物化执行: skill={}, execDir={}",
                    skill.getName(), execDir);

            if (ExecutionContext.FORK == skill.getExecutionContext()) {
                return executeFork(skill, processedContent, args, agentContext,
                        contextPatch, persistContent);
            } else {
                return executeInline(skill, processedContent, contextPatch, persistContent);
            }
        } finally {
            // 执行层工作目录：执行结束（含异常）后立即清理
            workspaceManager.releaseExecDir(execDir);
        }
    }

    /**
     * 纯文本执行路径（只有 SKILL.md，无附属文件）：直接获取 content。
     */
    private SkillToolData executeWithContentOnly(SkillDefinition skill, String args,
                                                  String sessionKey, AgentContext agentContext) {
        String content = loadContent(skill, sessionKey);
        String processedContent = injectArgs(content, args);
        ContextPatch contextPatch = buildContextPatch(skill, null);
        String persistContent = resolvePersistContent(skill, processedContent);

        if (ExecutionContext.FORK == skill.getExecutionContext()) {
            return executeFork(skill, processedContent, args, agentContext,
                    contextPatch, persistContent);
        } else {
            return executeInline(skill, processedContent, contextPatch, persistContent);
        }
    }

    // ── inline 执行（设计点 4）────────────────────────────────────────

    /**
     * inline 模式：将完整 prompt 包装为用户消息注入当前对话。
     * 模型接收到消息后直接在当前 context 中处理，无需创建新 Agent。
     */
    private SkillToolData executeInline(SkillDefinition skill, String processedContent,
                                         ContextPatch contextPatch, String persistContent) {
        // 构建 newMessages：将 skill prompt 包装为一条 meta 用户消息
        // 消息格式与框架的 Message 体系对齐，此处用 Map 表示结构
        // 框架层将其转为实际的 UserMessage 对象
        Map<String, Object> skillMessage = Map.of(
                "role", "user",
                "content", processedContent,
                "meta", true,                    // 标记为 meta 消息，不显示在用户界面
                "skillName", skill.getName()
        );

        log.debug("[SkillExecutor] inline 执行完成: skill={}, contentLen={}",
                skill.getName(), processedContent.length());

        return SkillToolData.builder()
                .skillName(skill.getName())
                .executionMode(SkillToolData.ExecutionMode.INLINE)
                .contextPatch(contextPatch)
                .newMessages(List.of(skillMessage))
                .persistContent(persistContent)
                .build();
    }

    // ── fork 执行（设计点 4）─────────────────────────────────────────

    /**
     * fork 模式：组装子 Agent 执行参数，委托框架层创建 AgentTask。
     *
     * <p>此方法不直接创建 Task（避免依赖具体的 TaskService），
     * 而是将执行参数打包到 SkillToolData 中，框架层识别 FORK 模式后负责创建。
     * 对应 TS 版本的 {@code executeForkedSkill()} → {@code runAgent()}。
     */
    private SkillToolData executeFork(SkillDefinition skill, String processedContent,
                                       String args, AgentContext agentContext,
                                       ContextPatch contextPatch, String persistContent) {
        log.debug("[SkillExecutor] fork 执行: skill={}, parentAgentId={}",
                skill.getName(), agentContext != null ? agentContext.getAgentId() : "null");

        // fork 模式下 newMessages 为 null，结果由子 AgentTask 异步汇报
        // 框架层通过 SkillToolData.executionMode=FORK 知道需要创建子任务
        return SkillToolData.builder()
                .skillName(skill.getName())
                .executionMode(SkillToolData.ExecutionMode.FORK)
                .contextPatch(contextPatch)
                .newMessages(null)
                // fork 子任务执行完毕后，框架层应将 persistContent 写入父 session
                .persistContent(persistContent)
                // 将执行参数存在 data 扩展字段中，供框架层构建 AgentTask 使用
                // 框架层取出后创建：AgentTask(prompt=processedContent, args=args,
                //                             allowedTools=skill.getAllowedTools(),
                //                             model=skill.getModel(),
                //                             effort=skill.getEffort(),
                //                             parentAgentId=agentContext.getAgentId())
                .build();
    }

    // ── ContextPatch 构建（设计点 5）─────────────────────────────────

    /**
     * 构建 ContextPatch：声明 Skill 执行期间需要的工具权限、模型、effort 变更。
     * 框架层负责"应用 → 执行 → 恢复"的生命周期管理。
     */
    /**
     * @param workDir 工作目录绝对路径（物化路径时传入）或 null（纯文本路径）
     */
    private ContextPatch buildContextPatch(SkillDefinition skill, String workDir) {
        List<String> tools = skill.getAllowedTools();
        String model = skill.getModel();
        SkillEffort effort = skill.getEffort();

        boolean hasTools   = tools != null && !tools.isEmpty();
        boolean hasModel   = StringUtils.hasText(model);
        boolean hasEffort  = effort != null;
        boolean hasWorkDir = StringUtils.hasText(workDir);

        if (!hasTools && !hasModel && !hasEffort && !hasWorkDir) {
            return null;  // 无需变更上下文
        }

        return ContextPatch.builder()
                .additionalAllowedTools(hasTools ? tools : null)
                .modelOverride(hasModel ? model : null)
                .effortOverride(hasEffort ? effort : null)
                .skillWorkDir(hasWorkDir ? workDir : null)
                .build();
    }

    // ── persist 处理（设计点 6）──────────────────────────────────────

    /**
     * 根据 Skill 的 persist/persistMode 决定需要持续留存的内容片段。
     *
     * @param skill            Skill 定义
     * @param processedContent 已注入 args 的完整 prompt 正文
     * @return 需要留存的内容；若 persist=false 则返回 null
     */
    private String resolvePersistContent(SkillDefinition skill, String processedContent) {
        if (!skill.isPersist()) {
            return null;
        }

        if (PersistMode.SUMMARY.equals(skill.getPersistMode())) {
            // 只提取约束/规则部分（节省 token）
            String summary = SkillDirectoryBuilder.extractSummary(processedContent);
            log.debug("[SkillExecutor] persist summary 提取: skill={}, summaryLen={}",
                    skill.getName(), summary.length());
            return summary;
        }

        // full 模式：留存完整正文
        return processedContent;
    }

    // ── content 懒加载与缓存（设计点 2）──────────────────────────────

    /**
     * 获取 Skill 完整 prompt 内容，并在本次对话（sessionKey）维度缓存。
     * 同一对话内多次调用同一 Skill 不重复获取。
     */
    private String loadContent(SkillDefinition skill, String sessionKey) {
        String cacheKey = sessionKey + ":" + skill.getSkillId();
        return contentCache.computeIfAbsent(cacheKey, k -> {
            String content = skill.getContent();
            log.debug("[SkillExecutor] content 已加载: skill={}, len={}", skill.getName(), content.length());
            return content;
        });
    }

    /**
     * 将 prompt 中的 {@code $ARGUMENTS} 占位符替换为实际参数。
     * 若 args 为空，移除占位符（避免模型看到字面量 $ARGUMENTS）。
     */
    private String injectArgs(String content, String args) {
        if (!StringUtils.hasText(content)) return content;
        String placeholder = "$ARGUMENTS";
        if (content.contains(placeholder)) {
            return content.replace(placeholder, args != null ? args : "");
        }
        // 没有占位符时，若有 args 则追加到末尾
        if (StringUtils.hasText(args)) {
            return content + "\n\n" + args;
        }
        return content;
    }

    /**
     * 清除指定 session 的 content 缓存（对话结束时调用，释放内存）。
     */
    public void clearSessionCache(String sessionKey) {
        contentCache.entrySet().removeIf(e -> e.getKey().startsWith(sessionKey + ":"));
    }

    /**
     * 从物化工作目录中读取 SKILL.md 文本内容。
     */
    private String readSkillMdFromDir(Path execDir) {
        Path skillMdPath = execDir.resolve("SKILL.md");
        try {
            return Files.readString(skillMdPath);
        } catch (IOException e) {
            throw new RuntimeException(
                    "SKILL.md 读取失败: " + skillMdPath + "，原因: " + e.getMessage(), e);
        }
    }
}


package org.dragon.skill.runtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.skill.dto.StorageInfo;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;

import java.util.List;

/**
 * Skill 执行定义（内存对象，非 DB 实体）。
 *
 * <p>由 SkillRegistry 从 DB 加载后聚合，供 SkillFilter、SkillDirectoryBuilder、SkillExecutor 使用。
 *
 * <p>仅包含 Skill 执行所需的信息，不包含管理端元数据（如 creatorId、createdAt、publishedAt 等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinition {

    // ── 标识 ─────────────────────────────────────────────────────────

    /** 技能 ID */
    private String skillId;

    /** 技能调用名称（/name 触发） */
    private String name;

    /** 当前有效版本号 */
    private int version;

    /** 技能分类 */
    private SkillCategory category;

    // ── 目录字段（注入 system prompt 目录部分，不含完整正文） ──────────

    /** 描述 */
    private String description;

    /**
     * 使用场景说明。
     * 驱动模型自动判断"何时应该调用此 Skill"，注入目录 prompt。
     */
    private String whenToUse;

    /** 参数提示（如 "[scope] &lt;branch&gt;"），注入目录 prompt */
    private String argumentHint;

    /** 别名列表 */
    private List<String> aliases;

    // ── 执行行为 ─────────────────────────────────────────────────────

    /** 允许使用的工具列表 */
    private List<String> allowedTools;

    /** 指定模型（覆盖默认），null 表示不覆盖 */
    private String model;

    /** 努力程度，null 表示不覆盖 */
    private SkillEffort effort;

    /**
     * 执行上下文。
     * {@link ExecutionContext#INLINE} 在当前对话展开；
     * {@link ExecutionContext#FORK} 创建独立 sub-AgentTask。
     */
    private ExecutionContext executionContext;

    /** 是否禁用模型自动调用 */
    private boolean disableModelInvocation;

    /** 用户是否可通过 /name 手动调用 */
    private boolean userInvocable;

    // ── 持续留存 ─────────────────────────────────────────────────────

    /**
     * 调用后是否持续留存在上下文。
     * true 时每轮对话将内容以 &lt;skill-context&gt; 形式重新注入。
     */
    private boolean persist;

    /**
     * 留存模式（persist=true 时生效）。
     * {@link PersistMode#FULL} 注入完整正文；
     * {@link PersistMode#SUMMARY} 只注入约束片段。
     */
    private PersistMode persistMode;

    // ── 标签 ───────────────────────────────────────────────────────

    /** 标签列表 */
    private List<String> tags;

    // ── 存储元信息 ────────────────────────────────────────────────

    /**
     * 存储元信息。
     *
     * <p>在 SkillExecutor 执行前，{@link SkillWorkspaceManager} 会检查此字段：
     * <ul>
     *   <li>若 {@code files.size() > 1}（含附属文件），则将所有文件物化到本地执行工作目录</li>
     *   <li>若只有 SKILL.md，则直接读取文本，无需物化</li>
     * </ul>
     */
    private StorageInfo storageInfo;

    // ── 正文内容 ───────────────────────────────────────────────────

    /** 完整 prompt 正文 */
    private String content;
}


package org.dragon.skill.runtime;

import org.dragon.skill.domain.StorageInfoVO;
import org.dragon.skill.enums.ExecutionContext;
import org.dragon.skill.enums.PersistMode;
import org.dragon.skill.enums.SkillCategory;
import org.dragon.skill.enums.SkillEffort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Skill 运行时定义（内存对象，非 DB 实体）。
 *
 * <p>由 SkillRegistry 从 DB 加载后聚合，供 SkillFilter、SkillDirectoryBuilder、SkillExecutor 使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinition {

    // ── 分类 ──────────────────────────────────────────────────────────

    /**
     * 技能分类。{@link SkillCategory#BUILTIN} 类型对所有 Character 全量可见，不走绑定关系。
     */
    private SkillCategory category;

    // ── 标识 ─────────────────────────────────────────────────────────

    /** 技能业务 UUID */
    private String skillId;

    /** 技能调用名称（/name 触发） */
    private String name;

    /** 当前有效版本号 */
    private int version;

    // ── 目录字段（注入 system prompt 目录部分，不含完整正文） ──────────

    /** 展示名称 */
    private String displayName;

    /** 描述 */
    private String description;

    /**
     * 使用场景说明（设计点 2）。
     * 驱动模型自动判断"何时应该调用此 Skill"，注入目录 prompt。
     */
    private String whenToUse;

    /** 参数提示（如 "[scope] &lt;branch&gt;"），注入目录 prompt */
    private String argumentHint;

    /** 别名列表 */
    private List<String> aliases;

    // ── 执行行为 ─────────────────────────────────────────────────────

    /**
     * 允许使用的工具列表（细粒度权限，设计点 5）。
     * 如 ["Bash(git:*)", "FileRead", "Grep"]。
     */
    private List<String> allowedTools;

    /** 指定模型（覆盖默认），null 表示不覆盖 */
    private String model;

    /** 努力程度，null 表示不覆盖 */
    private SkillEffort effort;

    /**
     * 执行上下文（设计点 4）。
     * {@link ExecutionContext#INLINE} 在当前对话展开；
     * {@link ExecutionContext#FORK} 创建独立 sub-AgentTask。
     */
    private ExecutionContext executionContext;

    /** 是否禁用模型自动调用 */
    private boolean disableModelInvocation;

    /** 用户是否可通过 /name 手动调用 */
    private boolean userInvocable;

    // ── 持续留存（设计点 6） ──────────────────────────────────────────

    /**
     * 调用后是否持续留存在上下文（设计点 6）。
     * true 时每轮对话将内容以 &lt;skill-context&gt; 形式重新注入，防止模型"遗忘"强制约束规则。
     */
    private boolean persist;

    /**
     * 留存模式（persist=true 时生效）。
     * {@link PersistMode#FULL} 注入完整正文；
     * {@link PersistMode#SUMMARY} 只注入约束片段（节省 token）。
     */
    private PersistMode persistMode;

    // ── 标签 ────────────────────────────────────────────────────────

    /** 标签列表，用于技能分类/场景归纳 */
    private List<String> tags;

    // ── 存储元信息（执行工作区物化用） ──────────────────────────────

    /**
     * 存储元信息（由 storage_info 字段反序列化而来）。
     *
     * <p>在 SkillExecutor 执行前，{@link SkillWorkspaceManager} 会检查此字段：
     * <ul>
     *   <li>若 {@code files.size() > 1}（含附属文件），则将所有文件物化到本地执行工作目录</li>
     *   <li>若只有 SKILL.md，则直接读取文本，无需物化</li>
     * </ul>
     */
    private StorageInfoVO storageInfo;

    // ── 正文内容 ───────────────────────────────────────────────────

    /** 完整 prompt 正文 */
    private String content;

    /** 是否是内置 Skill（category = BUILTIN） */
    public boolean isBuiltin() {
        return SkillCategory.BUILTIN == category;
    }
}


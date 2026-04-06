package org.dragon.config.enums;

/**
 * 配置粒度层级
 *
 * <p>共 45 个固定粒度：
 * <ul>
 *   <li>15 个系统级粒度（GLOBAL -> XXX）</li>
 *   <li>15 个用户级粒度（GLOBAL -> STUDIO -> XXX）</li>
 *   <li>15 个 OBSERVER 粒度（GLOBAL -> OBSERVER -> XXX）</li>
 * </ul>
 *
 * <p>每个粒度的 scopeBit = 其完整继承链上所有层级的 bit OR 组合。
 * 继承关系通过 {@link #isDescendantOf(ConfigLevel)} 检查。
 */
public enum ConfigLevel {

    // ==================== 系统级粒度 (scopeBit 1-15) ====================
    // 继承链：GLOBAL -> XXX

    GLOBAL_WORKSPACE(5, "GLOBAL -> WORKSPACE"),
    GLOBAL_CHARACTER(9, "GLOBAL -> CHARACTER"),
    GLOBAL_SKILL(17, "GLOBAL -> SKILL"),
    GLOBAL_TOOL(33, "GLOBAL -> TOOL"),
    GLOBAL_MEMORY(65, "GLOBAL -> MEMORY"),
    GLOBAL_WS_CHAR(13, "GLOBAL -> WORKSPACE -> CHARACTER"),
    GLOBAL_WS_SKILL(21, "GLOBAL -> WORKSPACE -> SKILL"),
    GLOBAL_WS_MEMORY(69, "GLOBAL -> WORKSPACE -> MEMORY"),
    GLOBAL_WS_TOOL(37, "GLOBAL -> WORKSPACE -> TOOL"),
    GLOBAL_CHAR_TOOL(41, "GLOBAL -> CHARACTER -> TOOL"),
    GLOBAL_CHAR_SKILL(25, "GLOBAL -> CHARACTER -> SKILL"),
    GLOBAL_CHAR_MEMORY(73, "GLOBAL -> CHARACTER -> MEMORY"),
    GLOBAL_WS_CHAR_TOOL(45, "GLOBAL -> WORKSPACE -> CHARACTER -> TOOL"),
    GLOBAL_WS_CHAR_SKILL(29, "GLOBAL -> WORKSPACE -> CHARACTER -> SKILL"),
    GLOBAL_WS_CHAR_MEMORY(77, "GLOBAL -> WORKSPACE -> CHARACTER -> MEMORY"),

    // ==================== 用户级粒度 (scopeBit 16-30) ====================
    // 继承链：GLOBAL -> STUDIO -> XXX
    // scopeBit = 系统级 + STUDIO bit (2)

    STUDIO_WORKSPACE(7, "GLOBAL -> STUDIO -> WORKSPACE"),
    STUDIO_CHARACTER(11, "GLOBAL -> STUDIO -> CHARACTER"),
    STUDIO_SKILL(19, "GLOBAL -> STUDIO -> SKILL"),
    STUDIO_TOOL(35, "GLOBAL -> STUDIO -> TOOL"),
    STUDIO_MEMORY(67, "GLOBAL -> STUDIO -> MEMORY"),
    STUDIO_WS_CHAR(15, "GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER"),
    STUDIO_WS_SKILL(23, "GLOBAL -> STUDIO -> WORKSPACE -> SKILL"),
    STUDIO_WS_MEMORY(71, "GLOBAL -> STUDIO -> WORKSPACE -> MEMORY"),
    STUDIO_WS_TOOL(39, "GLOBAL -> STUDIO -> WORKSPACE -> TOOL"),
    STUDIO_CHAR_TOOL(43, "GLOBAL -> STUDIO -> CHARACTER -> TOOL"),
    STUDIO_CHAR_SKILL(27, "GLOBAL -> STUDIO -> CHARACTER -> SKILL"),
    STUDIO_CHAR_MEMORY(75, "GLOBAL -> STUDIO -> CHARACTER -> MEMORY"),
    STUDIO_WS_CHAR_TOOL(47, "GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER -> TOOL"),
    STUDIO_WS_CHAR_SKILL(31, "GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER -> SKILL"),
    STUDIO_WS_CHAR_MEMORY(79, "GLOBAL -> STUDIO -> WORKSPACE -> CHARACTER -> MEMORY"),

    // ==================== OBSERVER 粒度 (scopeBit 31-45) ====================
    // 继承链：GLOBAL -> OBSERVER -> XXX
    // scopeBit = 系统级 + OBSERVER bit (128)

    OBSERVER_GLOBAL_WORKSPACE(133, "GLOBAL -> OBSERVER -> WORKSPACE"),
    OBSERVER_GLOBAL_CHARACTER(137, "GLOBAL -> OBSERVER -> CHARACTER"),
    OBSERVER_GLOBAL_SKILL(145, "GLOBAL -> OBSERVER -> SKILL"),
    OBSERVER_GLOBAL_TOOL(161, "GLOBAL -> OBSERVER -> TOOL"),
    OBSERVER_GLOBAL_MEMORY(193, "GLOBAL -> OBSERVER -> MEMORY"),
    OBSERVER_GLOBAL_WS_CHAR(141, "GLOBAL -> OBSERVER -> WORKSPACE -> CHARACTER"),
    OBSERVER_GLOBAL_WS_SKILL(149, "GLOBAL -> OBSERVER -> WORKSPACE -> SKILL"),
    OBSERVER_GLOBAL_WS_MEMORY(197, "GLOBAL -> OBSERVER -> WORKSPACE -> MEMORY"),
    OBSERVER_GLOBAL_WS_TOOL(165, "GLOBAL -> OBSERVER -> WORKSPACE -> TOOL"),
    OBSERVER_GLOBAL_CHAR_TOOL(169, "GLOBAL -> OBSERVER -> CHARACTER -> TOOL"),
    OBSERVER_GLOBAL_CHAR_SKILL(153, "GLOBAL -> OBSERVER -> CHARACTER -> SKILL"),
    OBSERVER_GLOBAL_CHAR_MEMORY(201, "GLOBAL -> OBSERVER -> CHARACTER -> MEMORY"),
    OBSERVER_GLOBAL_WS_CHAR_TOOL(173, "GLOBAL -> OBSERVER -> WORKSPACE -> CHARACTER -> TOOL"),
    OBSERVER_GLOBAL_WS_CHAR_SKILL(157, "GLOBAL -> OBSERVER -> WORKSPACE -> CHARACTER -> SKILL"),
    OBSERVER_GLOBAL_WS_CHAR_MEMORY(205, "GLOBAL -> OBSERVER -> WORKSPACE -> CHARACTER -> MEMORY");

    private final int scopeBit;
    private final String description;

    ConfigLevel(int scopeBit, String description) {
        this.scopeBit = scopeBit;
        this.description = description;
    }

    public int getScopeBit() {
        return scopeBit;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查当前粒度是否继承自指定粒度（通过 AND 运算）
     *
     * <p>如果 (this.scopeBit & ancestor.scopeBit) == ancestor.scopeBit，则继承成立。
     */
    public boolean isDescendantOf(ConfigLevel ancestor) {
        return (this.scopeBit & ancestor.scopeBit) == ancestor.scopeBit;
    }

    /**
     * 检查是否为用户级粒度（包含 STUDIO）
     */
    public boolean isStudio() {
        return (this.scopeBit & ScopeBits.STUDIO) == ScopeBits.STUDIO;
    }

    /**
     * 检查是否为 OBSERVER 粒度
     */
    public boolean isObserver() {
        return (this.scopeBit & ScopeBits.OBSERVER) == ScopeBits.OBSERVER;
    }

    /**
     * 从 scopeBit 反查枚举
     */
    public static ConfigLevel fromScopeBit(int scopeBit) {
        for (ConfigLevel level : values()) {
            if (level.scopeBit == scopeBit) {
                return level;
            }
        }
        return null;
    }
}
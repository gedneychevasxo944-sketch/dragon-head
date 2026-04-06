package org.dragon.config.enums;

/**
 * 配置层级位掩码
 *
 * <p>使用二进制位表示激活的层级，支持灵活的层级组合：
 * <ul>
 *   <li>Bit 0: GLOBAL - 全局配置</li>
 *   <li>Bit 1: STUDIO - 用户/租户级别</li>
 *   <li>Bit 2: WORKSPACE - 工作空间级别</li>
 *   <li>Bit 3: CHARACTER - 角色级别</li>
 *   <li>Bit 4: TOOL - 工具级别</li>
 *   <li>Bit 5: SKILL - 技能级别</li>
 *   <li>Bit 6: MEMORY - 记忆级别</li>
 *   <li>Bit 7: OBSERVER - 观察者级别</li>
 *   <li>Bit 8: MEMBER - 成员级别</li>
 * </ul>
 *
 * <p>示例：
 * <ul>
 *   <li>Global 配置: scopeBits = 0b00000001 (1)</li>
 *   <li>Workspace 配置: scopeBits = 0b00000101 (5) = GLOBAL | WORKSPACE</li>
 *   <li>Workspace+Character+Tool: scopeBits = 0b00011101 (29) = GLOBAL | WORKSPACE | CHARACTER | TOOL</li>
 * </ul>
 */
public final class ScopeBits {

    private ScopeBits() {}

    /** 全局配置 */
    public static final int GLOBAL = 0b00000001;

    /** 用户/租户级别 */
    public static final int STUDIO = 0b00000010;

    /** 工作空间级别 */
    public static final int WORKSPACE = 0b00000100;

    /** 角色级别 */
    public static final int CHARACTER = 0b00001000;

    /** 工具级别 */
    public static final int TOOL = 0b00010000;

    /** 技能级别 */
    public static final int SKILL = 0b00100000;

    /** 记忆级别 */
    public static final int MEMORY = 0b01000000;

    /** 观察者级别 */
    public static final int OBSERVER = 0b10000000;

    /** 成员级别 */
    public static final int MEMBER = 0b000100000;

    /** 存储所有有效位的掩码 */
    public static final int ALL_BITS = GLOBAL | STUDIO | WORKSPACE | CHARACTER | TOOL | SKILL | MEMORY | OBSERVER | MEMBER;

    /**
     * 根据层级类型获取对应的位掩码
     */
    public static int of(ConfigScopeType scopeType) {
        return switch (scopeType) {
            case GLOBAL -> GLOBAL;
            case STUDIO -> STUDIO;
            case WORKSPACE -> WORKSPACE;
            case CHARACTER -> CHARACTER;
            case TOOL -> TOOL;
            case SKILL -> SKILL;
            case MEMORY -> MEMORY;
            case OBSERVER -> OBSERVER;
            case MEMBER -> MEMBER;
            default -> 0;
        };
    }

    /**
     * 检查是否包含指定层级
     */
    public static boolean hasBit(int scopeBits, int bit) {
        return (scopeBits & bit) == bit;
    }

    /**
     * 检查是否包含指定层级类型
     */
    public static boolean hasScope(int scopeBits, ConfigScopeType scopeType) {
        return hasBit(scopeBits, of(scopeType));
    }

    /**
     * 组合多个层级
     */
    public static int combine(ConfigScopeType... types) {
        int bits = 0;
        for (ConfigScopeType type : types) {
            bits |= of(type);
        }
        return bits;
    }

    /**
     * 获取最低激活的层级
     */
    public static ConfigScopeType getLowestBit(int scopeBits) {
        if (hasBit(scopeBits, GLOBAL)) return ConfigScopeType.GLOBAL;
        if (hasBit(scopeBits, STUDIO)) return ConfigScopeType.STUDIO;
        if (hasBit(scopeBits, WORKSPACE)) return ConfigScopeType.WORKSPACE;
        if (hasBit(scopeBits, CHARACTER)) return ConfigScopeType.CHARACTER;
        if (hasBit(scopeBits, TOOL)) return ConfigScopeType.TOOL;
        if (hasBit(scopeBits, SKILL)) return ConfigScopeType.SKILL;
        if (hasBit(scopeBits, MEMORY)) return ConfigScopeType.MEMORY;
        if (hasBit(scopeBits, OBSERVER)) return ConfigScopeType.OBSERVER;
        if (hasBit(scopeBits, MEMBER)) return ConfigScopeType.MEMBER;
        return null;
    }

    /**
     * 获取最高激活的层级
     */
    public static ConfigScopeType getHighestBit(int scopeBits) {
        if (hasBit(scopeBits, MEMBER)) return ConfigScopeType.MEMBER;
        if (hasBit(scopeBits, OBSERVER)) return ConfigScopeType.OBSERVER;
        if (hasBit(scopeBits, MEMORY)) return ConfigScopeType.MEMORY;
        if (hasBit(scopeBits, SKILL)) return ConfigScopeType.SKILL;
        if (hasBit(scopeBits, TOOL)) return ConfigScopeType.TOOL;
        if (hasBit(scopeBits, CHARACTER)) return ConfigScopeType.CHARACTER;
        if (hasBit(scopeBits, WORKSPACE)) return ConfigScopeType.WORKSPACE;
        if (hasBit(scopeBits, STUDIO)) return ConfigScopeType.STUDIO;
        if (hasBit(scopeBits, GLOBAL)) return ConfigScopeType.GLOBAL;
        return null;
    }
}

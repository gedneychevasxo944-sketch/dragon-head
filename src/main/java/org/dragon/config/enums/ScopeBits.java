package org.dragon.config.enums;

/**
 * 配置层级 Bit 位掩码
 *
 * <p>每个基础层级占一个独立的 Bit 位，预留 bit 8-15 给未来扩展。
 * STUDIO 放在 bit 1，使得 scopeBit 大小可反映粒度粗细。
 *
 * <p>基础层级 Bit：
 * <ul>
 *   <li>bit 0: GLOBAL = 1</li>
 *   <li>bit 1: STUDIO = 2</li>
 *   <li>bit 2: WORKSPACE = 4</li>
 *   <li>bit 3: CHARACTER = 8</li>
 *   <li>bit 4: SKILL = 16</li>
 *   <li>bit 5: TOOL = 32</li>
 *   <li>bit 6: MEMORY = 64</li>
 *   <li>bit 7: OBSERVER = 128</li>
 *   <li>bit 8-15: 预留扩展（CommonSense=256, Prompt=512, ...）</li>
 * </ul>
 */
public final class ScopeBits {

    private ScopeBits() {}

    // ==================== 基础层级 Bit ====================

    /** 全局配置 */
    public static final int GLOBAL = 0b0000000000000001;  // 1

    /** 用户/租户级别 */
    public static final int STUDIO = 0b0000000000000010;  // 2

    /** 工作空间级别 */
    public static final int WORKSPACE = 0b0000000000000100;  // 4

    /** 角色级别 */
    public static final int CHARACTER = 0b0000000000001000;  // 8

    /** 技能级别 */
    public static final int SKILL = 0b0000000000010000;  // 16

    /** 工具级别 */
    public static final int TOOL = 0b0000000000100000;  // 32

    /** 记忆级别 */
    public static final int MEMORY = 0b0000000001000000;  // 64

    /** 观察者级别 */
    public static final int OBSERVER = 0b0000000010000000;  // 128

    // ==================== 预留扩展 Bit ====================

    /** 常识级别（预留） */
    public static final int COMMON_SENSE = 0b0000000100000000;  // 256

    /** Prompt 级别（预留） */
    public static final int PROMPT = 0b0000001000000000;  // 512

    // ==================== 辅助方法 ====================

    /**
     * 检查 scopeBit 是否包含 ancestorBit（即是否继承自 ancestor）
     *
     * <p>通过 AND 运算检查：如果 scopeBit 包含 ancestorBit 的所有位，则继承成立。
     */
    public static boolean isDescendantOf(int scopeBit, int ancestorBit) {
        return (scopeBit & ancestorBit) == ancestorBit;
    }
}
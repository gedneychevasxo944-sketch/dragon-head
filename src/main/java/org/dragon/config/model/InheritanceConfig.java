package org.dragon.config.model;

import org.dragon.config.enums.ConfigLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 简化版继承配置
 *
 * <p>用显式 Map 替代位运算来管理继承链路。
 *
 * <p>继承链含义：当某资产没有直接配置时，向上查找可用配置的路径。
 * 链路顺序：从具体到全局（前者优先级高）。
 *
 * <p>示例：TOOL → SKILL → USER → GLOBAL
 * <ul>
 *   <li>先查 TOOL 本身有没有配置</li>
 *   <li>没有 → 查 SKILL 有没有</li>
 *   <li>没有 → 查 USER 有没有</li>
 *   <li>没有 → 查 GLOBAL 的默认值</li>
 * </ul>
 */
public final class InheritanceConfig {

    private InheritanceConfig() {}

    // ==================== 层级定义 ====================

    /**
     * 简化版配置层级
     */
    public enum Level {
        GLOBAL,      // 全局默认值，最低优先级
        USER,        // 用户粒度
        WORKSPACE,   // 工作空间
        CHARACTER,   // 角色
        SKILL,       // 技能
        TOOL,        // 工具
        MEMORY       // 记忆
    }

    /**
     * 资产类型
     */
    public enum AssetType {
        WORKSPACE,
        CHARACTER,
        SKILL,
        TOOL,
        MEMORY
    }

    // ==================== 基础继承链路 ====================

    /**
     * 资产的基础继承链（不含父级时使用）
     * 从具体到全局
     *
     * <p>TOOL 的默认父级是 SKILL，所以基础链包含 SKILL
     */
    private static final Map<AssetType, List<Level>> BASE_CHAINS = Map.of(
        AssetType.WORKSPACE, List.of(Level.WORKSPACE, Level.USER, Level.GLOBAL),
        AssetType.CHARACTER, List.of(Level.CHARACTER, Level.USER, Level.GLOBAL),
        AssetType.SKILL,     List.of(Level.SKILL, Level.USER, Level.GLOBAL),
        AssetType.TOOL,      List.of(Level.TOOL, Level.SKILL, Level.USER, Level.GLOBAL),
        AssetType.MEMORY,    List.of(Level.MEMORY, Level.USER, Level.GLOBAL)
    );

    /**
     * 资产可以归属的上级层级
     */
    private static final Map<AssetType, List<Level>> PARENT_LEVELS = Map.of(
        AssetType.WORKSPACE, List.of(Level.USER),
        AssetType.CHARACTER, List.of(Level.USER, Level.WORKSPACE),
        AssetType.SKILL,     List.of(Level.USER, Level.WORKSPACE, Level.CHARACTER),
        AssetType.TOOL,      List.of(Level.USER, Level.WORKSPACE, Level.CHARACTER, Level.SKILL),
        AssetType.MEMORY,    List.of(Level.USER, Level.WORKSPACE, Level.CHARACTER)
    );

    // ==================== 公共接口 ====================

    /**
     * 获取资产的继承链（从具体到全局）
     */
    public static List<Level> getChain(AssetType assetType) {
        return BASE_CHAINS.get(assetType);
    }

    /**
     * 获取资产可归属的上级层级
     */
    public static List<Level> getParentLevels(AssetType assetType) {
        return PARENT_LEVELS.get(assetType);
    }

    /**
     * 根据父级构建完整继承链
     *
     * @param assetType 资产类型
     * @param parentLevel 父级（可为 null，表示资产直接归属 USER）
     * @return 完整继承链
     */
    public static List<Level> buildChain(AssetType assetType, Level parentLevel) {
        List<Level> baseChain = BASE_CHAINS.get(assetType);
        if (parentLevel == null || parentLevel == Level.GLOBAL) {
            return new ArrayList<>(baseChain);
        }

        List<Level> result = new ArrayList<>();
        result.add(baseChain.get(0)); // 资产自身

        int parentIndex = baseChain.indexOf(parentLevel);
        if (parentIndex > 0) {
            result.addAll(baseChain.subList(1, parentIndex + 1));
        } else {
            result.add(parentLevel);
            result.addAll(baseChain.subList(1, baseChain.size()));
        }

        return result;
    }

    // ==================== 与 ConfigLevel 的映射关系 ====================

    // ConfigLevel 的 scopeBit 与简化层级的对应关系
    // Bit: GLOBAL=1, STUDIO=2, WORKSPACE=4, CHARACTER=8, SKILL=16, TOOL=32, MEMORY=64
    private static final int GLOBAL_BIT = 1;
    private static final int STUDIO_BIT = 2;
    private static final int WORKSPACE_BIT = 4;
    private static final int CHARACTER_BIT = 8;
    private static final int SKILL_BIT = 16;
    private static final int TOOL_BIT = 32;
    private static final int MEMORY_BIT = 64;

    /**
     * 根据 ConfigLevel 获取对应的简化层级
     */
    public static Level toLevel(ConfigLevel configLevel) {
        int bit = configLevel.getScopeBit();

        // 检查是否是纯 USER 级别（STUDIO 但没有 WORKSPACE）
        if ((bit & STUDIO_BIT) != 0 && (bit & (WORKSPACE_BIT | CHARACTER_BIT | SKILL_BIT | TOOL_BIT | MEMORY_BIT)) == 0) {
            return Level.USER;
        }

        // 检查是否是 WORKSPACE 级别
        if ((bit & WORKSPACE_BIT) != 0 && (bit & (CHARACTER_BIT | SKILL_BIT | TOOL_BIT | MEMORY_BIT)) == 0) {
            return Level.WORKSPACE;
        }

        // 检查是否是 CHARACTER 级别
        if ((bit & CHARACTER_BIT) != 0 && (bit & (SKILL_BIT | TOOL_BIT | MEMORY_BIT)) == 0) {
            return Level.CHARACTER;
        }

        // 检查是否是 SKILL 级别
        if ((bit & SKILL_BIT) != 0 && (bit & (TOOL_BIT | MEMORY_BIT)) == 0) {
            return Level.SKILL;
        }

        // 检查是否是 TOOL 级别
        if ((bit & TOOL_BIT) != 0 && (bit & MEMORY_BIT) == 0) {
            return Level.TOOL;
        }

        // 检查是否是 MEMORY 级别
        if ((bit & MEMORY_BIT) != 0) {
            return Level.MEMORY;
        }

        // 默认是 GLOBAL
        return Level.GLOBAL;
    }

    /**
     * 检查 ConfigLevel 是否是全局级别（只有 GLOBAL 或 GLOBAL + STUDIO）
     */
    public static boolean isGlobalLevel(ConfigLevel configLevel) {
        int bit = configLevel.getScopeBit();
        return bit == GLOBAL_BIT || bit == (GLOBAL_BIT | STUDIO_BIT);
    }

    /**
     * 检查 ConfigLevel 是否有 workspace 父级
     */
    public static boolean hasWorkspaceParent(ConfigLevel configLevel) {
        int bit = configLevel.getScopeBit();
        return (bit & WORKSPACE_BIT) != 0;
    }

    /**
     * 检查 ConfigLevel 是否有 character 父级
     */
    public static boolean hasCharacterParent(ConfigLevel configLevel) {
        int bit = configLevel.getScopeBit();
        return (bit & CHARACTER_BIT) != 0;
    }

    /**
     * 检查 ConfigLevel 是否有 skill 父级
     */
    public static boolean hasSkillParent(ConfigLevel configLevel) {
        int bit = configLevel.getScopeBit();
        return (bit & SKILL_BIT) != 0;
    }
}

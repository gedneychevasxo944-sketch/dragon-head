package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资产配置项 VO
 *
 * <p>用于按资产查看配置时返回的数据，同一 configKey 的不同资产组合配置汇总在一起
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetConfigItemVO {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 生效值（当前查询上下文的最优先配置）
     *
     * <p>当存在多个可能的生效值且无法确定优先级时，此字段可能为空，
     * 具体候选值见 {@link #effectiveValueCandidates}
     */
    private Object effectiveValue;

    /**
     * 生效值的来源层级
     */
    private String sourceLevel;

    /**
     * 来源类型：OVERRIDDEN（直接配置）、INHERITED（继承）
     */
    private String sourceType;

    /**
     * 作用域标识
     */
    private Integer scopeBit;

    /**
     * 生效值是否模糊（存在多个可能的生效值）
     *
     * <p>当为 true 时，表示存在多个粒度同时命中配置且资产组合情况不明确，
     * 需要通过 {@link #effectiveValueCandidates} 列表来让用户选择
     */
    private boolean ambiguous;

    /**
     * 生效值候选列表
     *
     * <p>当存在多个可能的生效值时（ambiguous=true），此列表包含所有候选值，
     * 便于前端根据实际情况选择或展示差异
     */
    private List<CandidateValue> effectiveValueCandidates;

    /**
     * 资产组合配置列表
     *
     * <p>同一个 configKey 在不同资产组合下的所有配置值，
     * 按优先级排序（直接配置优先）
     */
    private List<ComboValue> comboValues;

    /**
     * 资产组合配置值
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComboValue {
        /**
         * 工作空间 ID（null 表示无工作空间上下文）
         */
        private String workspaceId;

        /**
         * 角色 ID（null 表示无角色上下文）
         */
        private String characterId;

        /**
         * 工具 ID
         */
        private String toolId;

        /**
         * 技能 ID
         */
        private String skillId;

        /**
         * 记忆 ID
         */
        private String memoryId;

        /**
         * 配置值
         */
        private Object value;

        /**
         * 来源层级名称
         */
        private String sourceLevel;

        /**
         * 来源类型：OVERRIDDEN（直接配置）、INHERITED（继承）
         */
        private String sourceType;

        /**
         * 是否为当前查询上下文的生效配置
         */
        private boolean effective;
    }

    /**
     * 生效值候选
     *
     * <p>当存在多个可能的生效值时，用于展示所有候选值及其来源信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateValue {
        /**
         * 候选值
         */
        private Object value;

        /**
         * 来源层级名称
         */
        private String sourceLevel;

        /**
         * 来源类型：OVERRIDDEN（直接配置）、INHERITED（继承）
         */
        private String sourceType;

        /**
         * 资产组合描述（如 "ws-demo/char-001" 或 "global"）
         */
        private String assetCombo;

        /**
         * 优先级（数值越小优先级越高）
         */
        private int priority;
    }
}
package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资产配置项 VO
 *
 * <p>用于 AssetConfigVO 中包含的单个配置项信息
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
     * 生效值
     */
    private Object effectiveValue;

    /**
     * 生效值的来源层级
     */
    private String sourceLevel;

    /**
     * 来源类型：OVERRIDDEN（本层配置）、INHERITED（继承）、NOT_SET（未设置）
     */
    private String sourceType;

    /**
     * 继承链（各层级是否有值）
     */
    private List<InheritanceChainNode> inheritanceChain;

    /**
     * 继承链节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InheritanceChainNode {
        /**
         * 层级名称
         */
        private String level;

        /**
         * 是否有值
         */
        private boolean hasValue;
    }
}
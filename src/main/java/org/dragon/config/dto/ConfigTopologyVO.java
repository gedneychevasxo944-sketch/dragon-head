package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 配置链路拓扑 VO
 *
 * <p>用于展示配置的完整继承链路（树状结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigTopologyVO {

    /**
     * 配置键（查询单个配置时）
     */
    private String configKey;

    /**
     * 链路节点列表（查询单个配置时）
     */
    private List<ConfigChainNodeVO> chain;

    /**
     * 完整拓扑树（不查 configKey 时返回）
     */
    private TopologyNodeVO tree;

    // ==================== 链路节点 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigChainNodeVO {
        /**
         * ConfigLevel 名称
         */
        private String level;

        /**
         * 层级显示名
         */
        private String levelName;

        /**
         * scopeBit
         */
        private int scopeBit;

        /**
         * 作用域 ID
         */
        private String scopeId;

        /**
         * 作用域 ID 类型（CHARACTER, WORKSPACE 等）
         */
        private String scopeIdType;

        /**
         * 该层是否有配置
         */
        private boolean hasConfig;

        /**
         * 配置值
         */
        private Object configValue;

        /**
         * 是否为最终生效值
         */
        private boolean isEffective;
    }

    // ==================== 拓扑节点 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopologyNodeVO {
        /**
         * ConfigLevel 名称
         */
        private String level;

        /**
         * 层级显示名
         */
        private String levelName;

        /**
         * scopeBit
         */
        private int scopeBit;

        /**
         * 工作空间 ID
         */
        private String workspaceId;

        /**
         * 角色 ID
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
         * 该层是否有配置
         */
        private boolean hasValue;

        /**
         * 配置值
         */
        private Object configValue;

        /**
         * 子节点列表
         */
        private List<TopologyNodeVO> children;
    }
}
package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 配置拓扑图 VO
 *
 * <p>用于前端图可视化，采用节点+边的结构（参考 lab topology）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigTopologyGraphVO {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 生效值
     */
    private Object effectiveValue;

    /**
     * 生效值来源层级
     */
    private String effectiveSource;

    /**
     * 拓扑节点列表
     */
    private List<ConfigGraphNodeVO> nodes;

    /**
     * 拓扑边列表
     */
    private List<ConfigGraphEdgeVO> edges;

    // ==================== 拓扑节点 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigGraphNodeVO {
        /**
         * 节点唯一标识
         */
        private String id;

        /**
         * 层级名称（如 GLOBAL, GLOBAL_WORKSPACE, GLOBAL_CHARACTER）
         */
        private String level;

        /**
         * 层级类型（如 WORKSPACE, CHARACTER, TOOL, SKILL, MEMORY, GLOBAL）
         */
        private String levelType;

        /**
         * 显示名称（如 "全局", "工作空间级别", "角色级别"）
         */
        private String name;

        /**
         * 所属配置键
         */
        private String configKey;

        /**
         * 该层是否设置了值
         */
        private boolean hasValue;

        /**
         * 配置值（如果 hasValue=true）
         */
        private Object value;

        /**
         * 是否为生效值来源
         */
        private boolean isEffective;

        /**
         * 场景值 Map：key 为资产上下文标识（如 workspaceId），value 为对应的值
         * 例如：{ "ws-1": 1000, "ws-2": 2000 }
         */
        private Map<String, Object> scenarioValues;

        /**
         * 节点 x 坐标（用于前端布局）
         */
        private Integer x;

        /**
         * 节点 y 坐标（用于前端布局）
         */
        private Integer y;

        /**
         * 节点大小：small, medium, large
         */
        private String size;

        /**
         * 状态：active, inactive, warning
         */
        private String status;
    }

    // ==================== 拓扑边 VO ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigGraphEdgeVO {
        /**
         * 边唯一标识
         */
        private String id;

        /**
         * 源节点 ID
         */
        private String source;

        /**
         * 目标节点 ID
         */
        private String target;

        /**
         * 边类型：inherits_from（继承）
         */
        private String type;

        /**
         * 边标签：继承
         */
        private String label;

        /**
         * 边的权重（0-1），用于显示边的粗细
         */
        private Double weight;
    }
}

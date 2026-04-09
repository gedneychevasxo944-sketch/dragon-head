package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资产类型选项 DTO
 *
 * <p>用于前端渐进式配置层级选择
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetTypeOption {

    /**
     * 资产类型标识
     */
    private String type;

    /**
     * 显示名称
     */
    private String label;

    /**
     * 可归属的上级层级列表
     */
    private List<String> parentLevels;

    /**
     * 图标名称
     */
    private String icon;
}

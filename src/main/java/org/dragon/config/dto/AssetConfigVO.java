package org.dragon.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 资产配置 VO
 *
 * <p>用于按资产查看配置时返回的数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetConfigVO {

    /**
     * 资产类型：CHARACTER, WORKSPACE, TOOL, SKILL, MEMORY
     */
    private String assetType;

    /**
     * 资产 ID
     */
    private String assetId;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 对应的 ConfigLevel 名称
     */
    private String scopeLevel;

    /**
     * 对应的 scopeBit
     */
    private int scopeBit;

    /**
     * 配置项列表
     */
    private List<AssetConfigItemVO> configs;
}
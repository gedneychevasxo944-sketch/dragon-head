package org.dragon.asset.tag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AssetTagDTO 资产标签数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetTagDTO {

    private String name;

    private String color;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
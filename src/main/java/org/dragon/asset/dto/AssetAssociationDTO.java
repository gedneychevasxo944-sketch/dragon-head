package org.dragon.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.asset.enums.AssociationType;
import org.dragon.permission.enums.ResourceType;

import java.time.LocalDateTime;

/**
 * AssetAssociationDTO 资产关联数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAssociationDTO {

    private Long id;

    private AssociationType associationType;

    private ResourceType sourceType;

    private String sourceId;

    private ResourceType targetType;

    private String targetId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

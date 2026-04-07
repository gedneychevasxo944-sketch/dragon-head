package org.dragon.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dragon.asset.enums.AssociationType;
import org.dragon.permission.enums.ResourceType;

/**
 * CreateAssociationRequest 创建资产关联请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAssociationRequest {

    private AssociationType associationType;

    private ResourceType sourceType;

    private String sourceId;

    private ResourceType targetType;

    private String targetId;
}

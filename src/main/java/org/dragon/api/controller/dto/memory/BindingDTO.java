package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 绑定关系 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BindingDTO {
    /**
     * 绑定唯一标识符
     */
    private String id;

    /**
     * 关联的文件 ID
     */
    private String fileId;

    /**
     * 目标类型：character/workspace
     */
    private String targetType;

    /**
     * 目标 ID
     */
    private String targetId;

    /**
     * 目标名称
     */
    private String targetName;

    /**
     * 挂载类型：full/selective/rule
     */
    private String mountType;

    /**
     * 选择性挂载的片段 ID 列表
     */
    private List<String> selectedChunkIds;

    /**
     * 规则挂载的规则列表
     */
    private List<MountRuleDTO> mountRules;

    /**
     * 挂载时间
     */
    private Instant mountedAt;

    /**
     * 挂载人
     */
    private String mountedBy;
}

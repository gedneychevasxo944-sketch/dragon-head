package org.dragon.api.controller.dto.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建绑定关系请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBindingRequest {
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
}

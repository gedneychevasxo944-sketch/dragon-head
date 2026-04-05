package org.dragon.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新绑定关系请求 DTO
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBindingRequest {
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

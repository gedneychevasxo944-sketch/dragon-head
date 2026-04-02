package org.dragon.memv2.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * 记忆查询DTO类
 * 抽象记忆检索请求，允许指定查询词、作用域、类型、返回上限等条件
 *
 * @author binarytom
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryQueryDTO {
    /**
     * 查询文本
     */
    private String text;

    /**
     * 工作空间ID
     */
    private String workspaceId;

    /**
     * 角色ID
     */
    private String characterId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 作用域集合
     */
    @Builder.Default
    private Set<String> scopes = new HashSet<>();

    /**
     * 类型集合
     */
    @Builder.Default
    private Set<String> types = new HashSet<>();

    /**
     * 返回结果数量限制
     */
    private int limit;
}

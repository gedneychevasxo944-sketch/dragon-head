package org.dragon.permission.enums;

/**
 * 资产成员角色
 */
public enum Role {
    OWNER,       // 所有者
    ADMIN,       // 管理员
    COLLABORATOR, // 协作者
    MEMBER,       // 成员（仅Workspace内资产）

    WILDCARD     //匹配任意角色
}

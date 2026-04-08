package org.dragon.skill.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Skill 绑定关系类型。
 *
 * <pre>
 * CHARACTER           : Character 自有 skill（characterId 必填，workspaceId=NULL）
 * WORKSPACE           : Workspace 公共 skill 池（workspaceId 必填，characterId=NULL）
 * CHARACTER_WORKSPACE : Character 在特定 Workspace 下的专属 skill（两者均必填）
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum BindingType {

    CHARACTER("character"),
    WORKSPACE("workspace"),
    CHARACTER_WORKSPACE("character_workspace");

    private final String value;

    public static BindingType fromValue(String value) {
        if (value == null) return null;
        for (BindingType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return null;
    }
}


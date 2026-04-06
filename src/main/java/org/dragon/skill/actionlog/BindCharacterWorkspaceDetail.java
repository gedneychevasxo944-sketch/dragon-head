package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 绑定到 Character@Workspace 的操作详情。
 */
@Data
@AllArgsConstructor
public class BindCharacterWorkspaceDetail implements ActionDetail {

    private String characterId;
    private String characterName;
    private String workspaceId;
    private String workspaceName;

    @Override
    public String getContent() {
        return "绑定到 Character [" + characterName + "] @ Workspace [" + workspaceName + "]";
    }
}

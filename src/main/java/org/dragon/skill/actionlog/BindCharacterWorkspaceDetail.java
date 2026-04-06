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
    private String versionType;
    private Integer fixedVersion;

    @Override
    public String getContent() {
        String ver = "latest".equals(versionType) ? "(latest)" : "(fixed v" + fixedVersion + ")";
        return "绑定到 Character [" + characterName + "] @ Workspace [" + workspaceName + "] " + ver;
    }
}

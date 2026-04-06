package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 绑定到 Workspace 的操作详情。
 */
@Data
@AllArgsConstructor
public class BindWorkspaceDetail implements ActionDetail {

    private String workspaceId;
    private String workspaceName;

    @Override
    public String getContent() {
        return "绑定到 Workspace [" + workspaceName + "]";
    }
}

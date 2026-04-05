package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 解绑操作详情。
 */
@Data
@AllArgsConstructor
public class UnbindDetail implements ActionDetail {

    private String bindingType;
    private String characterId;
    private String characterName;
    private String workspaceId;
    private String workspaceName;

    @Override
    public String getContent() {
        String target = switch (bindingType) {
            case "character" -> "Character [" + characterName + "]";
            case "workspace" -> "Workspace [" + workspaceName + "]";
            default -> "Character [" + characterName + "] @ Workspace [" + workspaceName + "]";
        };
        return "解除绑定 " + target;
    }
}

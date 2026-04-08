package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 绑定策略更新操作详情。
 */
@Data
@AllArgsConstructor
public class BindingUpdateDetail implements ActionDetail {

    private String bindingType;
    private String characterId;
    private String characterName;
    private String workspaceId;
    private String workspaceName;
    private String oldVersionType;
    private Integer oldFixedVersion;
    private String newVersionType;
    private Integer newFixedVersion;

    @Override
    public String getContent() {
        String target = switch (bindingType) {
            case "character" -> "Character [" + characterName + "]";
            case "workspace" -> "Workspace [" + workspaceName + "]";
            default -> "Character [" + characterName + "] @ Workspace [" + workspaceName + "]";
        };
        String oldVer = "latest".equals(oldVersionType) ? "latest" : "v" + oldFixedVersion;
        String newVer = "latest".equals(newVersionType) ? "latest" : "v" + newFixedVersion;
        return "更新 " + target + " 绑定策略：" + oldVer + " → " + newVer;
    }
}

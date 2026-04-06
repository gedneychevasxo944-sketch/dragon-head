package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 绑定到 Character 的操作详情。
 */
@Data
@AllArgsConstructor
public class BindCharacterDetail implements ActionDetail {

    private String characterId;
    private String characterName;

    @Override
    public String getContent() {
        return "绑定到 Character [" + characterName + "]";
    }
}

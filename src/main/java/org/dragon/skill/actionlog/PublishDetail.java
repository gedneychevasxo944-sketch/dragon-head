package org.dragon.skill.actionlog;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 发布技能版本的操作详情。
 */
@Data
@AllArgsConstructor
public class PublishDetail implements ActionDetail {

    private String releaseNote;

    @Override
    public String getContent() {
        if (releaseNote == null || releaseNote.isBlank()) {
            return "发布版本";
        }
        return "发布版本: " + releaseNote;
    }
}

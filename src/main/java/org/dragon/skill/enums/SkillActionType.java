package org.dragon.skill.enums;

/**
 * Skill 操作类型枚举。
 *
 * <p>用于记录 skill_action_log 表中的操作类型
 */
public enum SkillActionType {

    REGISTER("注册了技能"),
    UPDATE("更新了技能"),
    PUBLISH("发布了技能"),
    DISABLE("下架了技能"),
    REPUBLISH("重新发布了技能"),
    DELETE("删除了技能"),
    SAVE_DRAFT("保存了草稿"),
    BIND_CHARACTER("绑定到 Character"),
    BIND_WORKSPACE("绑定到 Workspace"),
    BIND_CHARACTER_WORKSPACE("绑定到 Character@Workspace"),
    UNBIND("解除绑定"),
    BINDING_UPDATE("更新绑定策略");

    private final String label;

    SkillActionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

package org.dragon.skill.model;

/**
 * 技能来源枚举。
 *
 * @since 1.0
 */
public enum SkillSource {
    BUNDLED("bundled"),
    MANAGED("managed"),
    WORKSPACE("workspace"),
    EXTRA("extra"),
    PLUGIN("plugin");

    private final String label;

    SkillSource(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

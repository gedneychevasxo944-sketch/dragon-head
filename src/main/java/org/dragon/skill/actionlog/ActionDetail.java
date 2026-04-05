package org.dragon.skill.actionlog;

/**
 * Skill 操作详情接口。
 *
 * <p>每种操作类型实现类定义自己的结构 + 日志描述生成逻辑。
 * 通过 JSON 序列化存储到 detail 字段。
 */
public interface ActionDetail {

    /**
     * 生成可读的操作描述，用于前端日志展示。
     *
     * @return 操作描述，如 "绑定到 Character [小助手] (latest)"
     */
    String getContent();
}

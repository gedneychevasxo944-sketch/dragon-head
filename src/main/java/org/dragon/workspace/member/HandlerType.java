package org.dragon.workspace.member;

/**
 * HandlerType 处理方式枚举
 * 定义 Scope 的处理方式
 *
 * @author yijunw
 * @version 1.0
 */
public enum HandlerType {

    /**
     * 由 Built-in Character 处理
     */
    BUILTIN_CHARACTER,

    /**
     * 由外部 Hook 处理
     */
    EXTERNAL_HOOK
}

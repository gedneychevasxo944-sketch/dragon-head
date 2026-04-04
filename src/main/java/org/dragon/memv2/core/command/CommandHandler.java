package org.dragon.memv2.core.command;

/**
 * 命令处理器接口
 *
 * @param <C> 命令类型
 * @param <R> 返回值类型
 * @author binarytom
 * @version 1.0
 */
public interface CommandHandler<C, R> {
    R handle(C command);
}

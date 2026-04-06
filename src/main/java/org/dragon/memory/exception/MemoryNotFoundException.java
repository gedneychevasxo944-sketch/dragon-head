package org.dragon.memory.exception;

/**
 * 记忆未找到异常类
 * 当找不到指定的记忆条目时抛出
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryNotFoundException extends MemoryException {

    public MemoryNotFoundException(String id) {
        super("记忆条目未找到: " + id);
    }

    public MemoryNotFoundException(String id, Throwable cause) {
        super("记忆条目未找到: " + id, cause);
    }
}

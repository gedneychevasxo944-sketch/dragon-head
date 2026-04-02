package org.dragon.memv2.exception;

/**
 * 记忆存储异常类
 * 当记忆存储操作失败时抛出
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryStorageException extends MemoryException {

    public MemoryStorageException(String message) {
        super("记忆存储失败: " + message);
    }

    public MemoryStorageException(String message, Throwable cause) {
        super("记忆存储失败: " + message, cause);
    }
}

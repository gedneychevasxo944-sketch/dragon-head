package org.dragon.memory.exception;

/**
 * 记忆验证异常类
 * 当记忆条目验证失败时抛出
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryValidationException extends MemoryException {

    public MemoryValidationException(String message) {
        super("记忆验证失败: " + message);
    }

    public MemoryValidationException(String message, Throwable cause) {
        super("记忆验证失败: " + message, cause);
    }
}

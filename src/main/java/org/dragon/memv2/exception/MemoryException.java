package org.dragon.memv2.exception;

/**
 * 记忆模块基础异常类
 * 所有记忆模块的业务异常都应该继承这个类
 *
 * @author binarytom
 * @version 1.0
 */
public class MemoryException extends RuntimeException {

    public MemoryException(String message) {
        super(message);
    }

    public MemoryException(String message, Throwable cause) {
        super(message, cause);
    }
}

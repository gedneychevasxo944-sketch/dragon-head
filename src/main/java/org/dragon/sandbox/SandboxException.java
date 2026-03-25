package org.dragon.sandbox;

/**
 * Sandbox 异常。
 *
 * @since 1.0
 */
public class SandboxException extends RuntimeException {

    public SandboxException(String message) {
        super(message);
    }

    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
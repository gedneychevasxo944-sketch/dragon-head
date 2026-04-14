package org.dragon.tool.enums;

/**
 * 工具执行状态枚举。
 */
public enum ExecutionStatus {

    /**
     * 执行中。
     */
    RUNNING("running"),

    /**
     * 执行成功。
     */
    SUCCESS("success"),

    /**
     * 执行失败。
     */
    FAILED("failed"),

    /**
     * 执行超时。
     */
    TIMEOUT("timeout"),

    /**
     * 用户取消。
     */
    CANCELLED("cancelled"),

    /**
     * 权限被拒绝。
     */
    PERMISSION_DENIED("permission_denied"),

    /**
     * 参数校验失败。
     */
    VALIDATION_ERROR("validation_error");

    private final String code;

    ExecutionStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public boolean isTerminal() {
        return this != RUNNING;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailed() {
        return this == FAILED || this == TIMEOUT || this == CANCELLED
                || this == PERMISSION_DENIED || this == VALIDATION_ERROR;
    }

    public static ExecutionStatus fromCode(String code) {
        for (ExecutionStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown execution status: " + code);
    }
}

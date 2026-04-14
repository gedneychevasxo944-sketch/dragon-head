package org.dragon.tool.runtime;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 输入校验结果。
 *
 * <p>对应 TypeScript 版本的 {@code ValidationResult} 类型：
 * <pre>
 * export type ValidationResult =
 *   | { result: true }
 *   | { result: false; message: string; errorCode: number }
 * </pre>
 */
@Data
@Builder
public class ValidationResult {

    /**
     * 校验是否通过。
     */
    private final boolean valid;

    /**
     * 错误消息（校验失败时）。
     */
    private final String message;

    /**
     * 错误码（可选）。
     */
    private final Integer errorCode;

    // ── 静态工厂方法 ─────────────────────────────────────────────────────

    /**
     * 创建成功结果。
     */
    public static ValidationResult ok() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }

    /**
     * 创建失败结果。
     */
    public static ValidationResult fail(String message) {
        return ValidationResult.builder()
                .valid(false)
                .message(message)
                .build();
    }

    /**
     * 创建失败结果（带错误码）。
     */
    public static ValidationResult fail(String message, int errorCode) {
        return ValidationResult.builder()
                .valid(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}

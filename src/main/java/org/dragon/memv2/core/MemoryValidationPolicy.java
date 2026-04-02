package org.dragon.memv2.core;

/**
 * 记忆校验策略接口
 * 负责验证记忆条目的有效性和完整性
 *
 * @author binarytom
 * @version 1.0
 */
public interface MemoryValidationPolicy {
    /**
     * 验证记忆条目是否有效
     *
     * @param entry 待验证的记忆条目
     * @return 验证结果，包含是否有效和错误信息
     */
    ValidationResult validate(MemoryEntry entry);

    /**
     * 验证结果类
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        /**
         * 创建有效的验证结果
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        /**
         * 创建无效的验证结果
         */
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

package org.dragon.schedule.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证结果
 */
@Data
public class ValidationResult {

    private boolean valid = true;
    private List<ValidationError> errors = new ArrayList<>();

    /**
     * 添加错误
     */
    public void addError(String field, String message) {
        this.valid = false;
        this.errors.add(new ValidationError(field, message));
    }

    /**
     * 是否验证通过
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        if (errors.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ValidationError error : errors) {
            sb.append("[").append(error.getField()).append("] ")
              .append(error.getMessage()).append("; ");
        }
        return sb.toString();
    }

    /**
     * 验证错误
     */
    @Data
    public static class ValidationError {
        private String field;
        private String message;

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}

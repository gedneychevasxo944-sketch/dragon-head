package org.dragon.memory.app;

import org.dragon.memory.core.MemoryEntry;
import org.dragon.memory.core.MemoryValidationPolicy;
import org.springframework.stereotype.Service;

/**
 * 记忆校验策略默认实现
 * 负责验证记忆条目的基本有效性和完整性
 *
 * @author binarytom
 * @version 1.0
 */
@Service
public class DefaultMemoryValidationPolicy implements MemoryValidationPolicy {

    @Override
    public ValidationResult validate(MemoryEntry entry) {
        // 验证必填字段
        if (entry == null) {
            return ValidationResult.invalid("记忆条目不能为空");
        }

        if (entry.getId() == null) {
            return ValidationResult.invalid("记忆ID不能为空");
        }

        if (entry.getTitle() == null || entry.getTitle().isEmpty()) {
            return ValidationResult.invalid("记忆标题不能为空");
        }

        if (entry.getContent() == null || entry.getContent().isEmpty()) {
            return ValidationResult.invalid("记忆内容不能为空");
        }

        if (entry.getType() == null) {
            return ValidationResult.invalid("记忆类型不能为空");
        }

        if (entry.getFileName() == null || entry.getFileName().isEmpty()) {
            return ValidationResult.invalid("文件名不能为空");
        }

        if (entry.getCreatedAt() == null) {
            return ValidationResult.invalid("创建时间不能为空");
        }

        // 验证文件名格式
        if (!entry.getFileName().endsWith(".md")) {
            return ValidationResult.invalid("文件名必须以.md结尾");
        }

        // 验证内容长度
        if (entry.getContent().length() > 10000) {
            return ValidationResult.invalid("记忆内容长度不能超过10000字符");
        }

        return ValidationResult.valid();
    }
}

package org.dragon.memory.entity;

import java.util.UUID;

/**
 * 记忆ID类
 * 统一管理记忆条目的标识符，提供类型安全的ID管理
 *
 * @author binarytom
 * @version 1.0
 */
public final class MemoryId {
    private final String value;

    private MemoryId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("MemoryId value cannot be null or empty");
        }
        this.value = value.trim();
    }

    /**
     * 从字符串创建MemoryId
     *
     * @param value 字符串值
     * @return MemoryId实例
     */
    public static MemoryId of(String value) {
        return new MemoryId(value);
    }

    /**
     * 生成新的UUID类型MemoryId
     *
     * @return 新生成的MemoryId
     */
    public static MemoryId generate() {
        return new MemoryId(UUID.randomUUID().toString());
    }

    /**
     * 从内容生成哈希ID
     *
     * @param content 内容字符串
     * @return 基于内容哈希的MemoryId
     */
    public static MemoryId fromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return generate();
        }
        String normalizedContent = content.toLowerCase().trim();
        int hash = normalizedContent.hashCode();
        String hexHash = Integer.toHexString(hash & 0xffffffff);
        return new MemoryId("hash_" + hexHash);
    }

    /**
     * 获取ID的字符串值
     *
     * @return ID字符串
     */
    public String getValue() {
        return value;
    }

    /**
     * 检查ID是否为UUID格式
     */
    public boolean isUUID() {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 检查ID是否为哈希格式
     */
    public boolean isHash() {
        return value.startsWith("hash_");
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MemoryId memoryId = (MemoryId) obj;
        return value.equals(memoryId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

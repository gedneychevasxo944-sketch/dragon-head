package org.dragon.tool.enums;

import io.ebean.annotation.DbEnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dragon.tool.domain.ToolVersionDO;

/**
 * Tool 文件存储类型。
 *
 * <p>决定 {@link ToolVersionDO#getStorageInfo()} 中路径的解析方式：
 * <ul>
 *   <li>{@link #LOCAL} - 本地磁盘，basePath 为绝对本地路径</li>
 *   <li>{@link #S3}    - AWS S3 或兼容对象存储，含 bucket 字段</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ToolStorageType {

    /** 本地磁盘存储（开发 / 测试环境） */
    LOCAL("local"),

    /** AWS S3 或兼容对象存储（生产环境） */
    S3("s3");

    private final String value;

    @DbEnumValue
    public String getValue() { return value; }

    /**
     * 从字符串值解析枚举，未知值默认返回 {@link #LOCAL}。
     */
    public static ToolStorageType fromValue(String value) {
        if (value == null) return LOCAL;
        for (ToolStorageType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return LOCAL;
    }
}


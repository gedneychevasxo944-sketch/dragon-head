package org.dragon.skill.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Skill 文件存储类型。 */
@Getter
@RequiredArgsConstructor
public enum StorageType {

    /** 本地磁盘存储 */
    LOCAL("local"),

    /** AWS S3 或兼容对象存储 */
    S3("s3");

    private final String value;

    public static StorageType fromValue(String value) {
        if (value == null) return LOCAL;
        for (StorageType t : values()) {
            if (t.value.equals(value)) return t;
        }
        return LOCAL;
    }
}


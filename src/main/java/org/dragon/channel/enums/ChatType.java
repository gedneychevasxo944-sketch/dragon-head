package org.dragon.channel.enums;

/**
 * 会话类型枚举
 *
 * @author yijunwang
 * @version 1.0
 */
public enum ChatType {
    P2P("p2p", "单聊"),
    GROUP("group", "群聊"),
    UNKNOWN("unknown", "未知类型");

    private final String code;
    private final String description;

    ChatType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code 查找枚举值
     *
     * @param code 会话类型编码
     * @return 对应的枚举值，未找到返回 UNKNOWN
     */
    public static ChatType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (ChatType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
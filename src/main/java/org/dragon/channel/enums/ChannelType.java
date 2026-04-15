package org.dragon.channel.enums;

/**
 * 渠道类型枚举
 *
 * @author yijunwang
 * @version 1.0
 */
public enum ChannelType {
    FEISHU("Feishu", "飞书"),
    TELEGRAM("Telegram", "Telegram"),
    WECHAT("Wechat", "微信"),
    UNKNOWN("unknown", "未知渠道");

    private final String code;
    private final String description;

    ChannelType(String code, String description) {
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
     * @param code 渠道编码
     * @return 对应的枚举值，未找到返回 UNKNOWN
     */
    public static ChannelType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (ChannelType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
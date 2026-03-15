package org.dragon.schedule.parser;

import lombok.Getter;

/**
 * Cron 字段类型
 */
@Getter
enum CronFieldType {

    SECOND(0, 59, "秒"),
    MINUTE(0, 59, "分"),
    HOUR(0, 23, "时"),
    DAY_OF_MONTH(1, 31, "日"),
    MONTH(1, 12, "月"),
    DAY_OF_WEEK(0, 7, "周"),
    YEAR(1970, 9999, "年");

    private final int min;
    private final int max;
    private final String displayName;

    CronFieldType(int min, int max, String displayName) {
        this.min = min;
        this.max = max;
        this.displayName = displayName;
    }
}

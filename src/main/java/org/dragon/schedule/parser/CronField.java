package org.dragon.schedule.parser;

import java.util.*;

/**
 * Cron 字段解析器
 */
class CronField {

    private final CronFieldType type;
    private final String expression;
    private final Set<Integer> values;
    private final int min;
    private final int max;

    public CronField(CronFieldType type, String expression) {
        this.type = type;
        this.expression = expression;
        this.min = type.getMin();
        this.max = type.getMax();
        this.values = parse(expression);
    }

    /**
     * 解析表达式
     */
    private Set<Integer> parse(String expr) {
        Set<Integer> result = new HashSet<>();

        // 处理特殊字符 L（仅用于日和周）
        if (expr.equals("L")) {
            if (type == CronFieldType.DAY_OF_MONTH) {
                result.add(-1); // 表示最后一天
                return result;
            }
            if (type == CronFieldType.DAY_OF_WEEK) {
                result.add(-1); // 表示最后一个星期
                return result;
            }
            throw new IllegalArgumentException("'L' can only be used in day-of-month or day-of-week");
        }

        // 处理问号（仅用于日和周）
        if (expr.equals("?")) {
            return result; // 返回空集，表示不指定
        }

        // 处理列表（逗号分隔）
        String[] parts = expr.split(",");
        for (String part : parts) {
            result.addAll(parsePart(part.trim()));
        }

        return result;
    }

    /**
     * 解析单个部分
     */
    private Set<Integer> parsePart(String part) {
        Set<Integer> result = new HashSet<>();

        // 处理步长（/）
        if (part.contains("/")) {
            String[] stepParts = part.split("/");
            String range = stepParts[0];
            int step = Integer.parseInt(stepParts[1]);

            int start, end;
            if (range.equals("*")) {
                start = min;
                end = max;
            } else if (range.contains("-")) {
                String[] rangeParts = range.split("-");
                start = Integer.parseInt(rangeParts[0]);
                end = Integer.parseInt(rangeParts[1]);
            } else {
                start = Integer.parseInt(range);
                end = max;
            }

            for (int i = start; i <= end; i += step) {
                result.add(i);
            }
        }
        // 处理范围（-）
        else if (part.contains("-")) {
            String[] rangeParts = part.split("-");
            int start = Integer.parseInt(rangeParts[0]);
            int end = Integer.parseInt(rangeParts[1]);
            for (int i = start; i <= end; i++) {
                result.add(i);
            }
        }
        // 处理通配符（*）
        else if (part.equals("*")) {
            for (int i = min; i <= max; i++) {
                result.add(i);
            }
        }
        // 处理具体值
        else {
            // 处理月份名称和星期名称
            int value = parseNamedValue(part);
            result.add(value);
        }

        return result;
    }

    /**
     * 解析命名值（月份和星期）
     */
    private int parseNamedValue(String value) {
        String upper = value.toUpperCase();

        // 月份名称
        if (type == CronFieldType.MONTH) {
            switch (upper) {
                case "JAN": case "JANUARY": return 1;
                case "FEB": case "FEBRUARY": return 2;
                case "MAR": case "MARCH": return 3;
                case "APR": case "APRIL": return 4;
                case "MAY": return 5;
                case "JUN": case "JUNE": return 6;
                case "JUL": case "JULY": return 7;
                case "AUG": case "AUGUST": return 8;
                case "SEP": case "SEPTEMBER": return 9;
                case "OCT": case "OCTOBER": return 10;
                case "NOV": case "NOVEMBER": return 11;
                case "DEC": case "DECEMBER": return 12;
            }
        }

        // 星期名称
        if (type == CronFieldType.DAY_OF_WEEK) {
            switch (upper) {
                case "SUN": case "SUNDAY": return 0;
                case "MON": case "MONDAY": return 1;
                case "TUE": case "TUESDAY": return 2;
                case "WED": case "WEDNESDAY": return 3;
                case "THU": case "THURSDAY": return 4;
                case "FRI": case "FRIDAY": return 5;
                case "SAT": case "SATURDAY": return 6;
            }
        }

        return Integer.parseInt(value);
    }

    /**
     * 检查值是否匹配
     */
    public boolean matches(int value) {
        // 空集表示不指定（?），总是匹配
        if (values.isEmpty()) {
            return true;
        }
        return values.contains(value);
    }

    /**
     * 是否包含所有值（*）
     */
    public boolean isAll() {
        return values.size() == (max - min + 1);
    }

    /**
     * 获取所有可能的值
     */
    public Set<Integer> getValues() {
        return new HashSet<>(values);
    }

    @Override
    public String toString() {
        return expression;
    }
}

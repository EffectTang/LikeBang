package com.likebang.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间工具类
 */
public final class DateTimeUtils {

    /**
     * 标准日期时间格式
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期时间格式化器
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private DateTimeUtils() {
        // 工具类禁止实例化
    }

    /**
     * 获取当前时间
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前时间字符串
     *
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static String nowString() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * LocalDateTime 转字符串
     *
     * @param dateTime 时间
     * @return yyyy-MM-dd HH:mm:ss
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 字符串转 LocalDateTime
     *
     * @param dateTime 时间字符串
     * @return LocalDateTime
     */
    public static LocalDateTime parse(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }

        return LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER);
    }
}

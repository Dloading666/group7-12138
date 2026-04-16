package com.spider.exc.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 指标日期工具类
 * 用于计算月份差、判断时间范围等
 */
public class IndicatorDateUtil {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 判断发票开票时间是否在申请日期前1-12个月之间（不含当月）
     * 
     * @param invoiceTimeStr 发票开票时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param appDateStr 申请日期字符串，格式：yyyy-MM-dd
     * @return true表示在范围内，false表示不在范围内
     */
    public static boolean isInDateRange(String invoiceTimeStr, String appDateStr) {
        try {
            LocalDate invoiceDate = LocalDate.parse(invoiceTimeStr.substring(0, 10), DATE_FORMATTER);
            LocalDate appDate = LocalDate.parse(appDateStr, DATE_FORMATTER);
            
            // 计算月份差（不含当月）
            // 例如：appDate = 2024-02-06，则范围是 2023-02-06 到 2024-01-06（不含2024-02-06）
            LocalDate startDate = appDate.minusMonths(12).withDayOfMonth(appDate.getDayOfMonth());
            LocalDate endDate = appDate.minusMonths(1).withDayOfMonth(appDate.getDayOfMonth());
            
            // 如果月份不足，调整日期
            if (startDate.getDayOfMonth() > startDate.lengthOfMonth()) {
                startDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            }
            if (endDate.getDayOfMonth() > endDate.lengthOfMonth()) {
                endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
            }
            
            // 判断是否在范围内（包含边界）
            return !invoiceDate.isBefore(startDate) && !invoiceDate.isAfter(endDate);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 计算发票开票时间与申请日期的月份差
     * 
     * @param invoiceTimeStr 发票开票时间字符串
     * @param appDateStr 申请日期字符串
     * @return 月份差（正数表示发票在申请日期之前）
     */
    public static long calculateMonthDifference(String invoiceTimeStr, String appDateStr) {
        try {
            LocalDate invoiceDate = LocalDate.parse(invoiceTimeStr.substring(0, 10), DATE_FORMATTER);
            LocalDate appDate = LocalDate.parse(appDateStr, DATE_FORMATTER);
            
            return ChronoUnit.MONTHS.between(invoiceDate, appDate);
        } catch (Exception e) {
            return Long.MAX_VALUE; // 解析失败返回最大值
        }
    }
    
    /**
     * 从发票开票时间提取月份（格式：yyyy-MM）
     * 
     * @param invoiceTimeStr 发票开票时间字符串
     * @return 月份字符串，格式：yyyy-MM
     */
    public static String extractMonth(String invoiceTimeStr) {
        try {
            return invoiceTimeStr.substring(0, 7); // 提取 yyyy-MM
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 格式化日期
     */
    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }
    
    /**
     * 解析日期字符串
     */
    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }
}

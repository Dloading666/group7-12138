package com.spider.exc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 指标统计计算工具类
 * 用于计算均值、标准差、波动系数等
 */
public class IndicatorStatUtil {
    
    private static final int SCALE = 5; // 保留5位小数
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    
    /**
     * 计算均值
     * 
     * @param values 数值列表
     * @return 均值（保留5位小数）
     */
    public static BigDecimal calculateMean(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING_MODE);
    }
    
    /**
     * 计算标准差
     * 
     * @param values 数值列表
     * @param mean 均值
     * @return 标准差（保留5位小数）
     */
    public static BigDecimal calculateStdDev(List<BigDecimal> values, BigDecimal mean) {
        if (values == null || values.isEmpty() || mean == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sumSquaredDiff = values.stream()
                .map(value -> value.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal variance = sumSquaredDiff.divide(
                BigDecimal.valueOf(values.size()), SCALE, ROUNDING_MODE);
        
        // 计算平方根
        double varianceDouble = variance.doubleValue();
        if (varianceDouble < 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(Math.sqrt(varianceDouble))
                .setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * 计算波动系数（变异系数）CV = 标准差 / 均值
     * 
     * @param stdDev 标准差
     * @param mean 均值
     * @return 波动系数（保留5位小数），如果均值为0或空，返回999
     */
    public static BigDecimal calculateCoefficientOfVariation(BigDecimal stdDev, BigDecimal mean) {
        if (mean == null || mean.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(999).setScale(SCALE, ROUNDING_MODE);
        }
        
        if (stdDev == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        }
        
        return stdDev.divide(mean, SCALE, ROUNDING_MODE);
    }
    
    /**
     * 四舍五入保留5位小数
     */
    public static BigDecimal roundToFiveDecimals(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        }
        return value.setScale(SCALE, ROUNDING_MODE);
    }
}

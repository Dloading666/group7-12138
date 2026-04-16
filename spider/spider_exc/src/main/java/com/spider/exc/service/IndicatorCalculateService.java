package com.spider.exc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spider.exc.domain.entity.TeachInvoice;
import com.spider.exc.domain.mapper.TeachInvoiceMapper;
import com.spider.exc.dto.Indicator1Result;
import com.spider.exc.dto.Indicator2Result;
import com.spider.exc.dto.Indicator3Result;
import com.spider.exc.util.IndicatorDateUtil;
import com.spider.exc.util.IndicatorStatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指标计算服务
 * 实现三个核心指标的计算逻辑
 */
@Service
public class IndicatorCalculateService {
    
    @Autowired
    private TeachInvoiceMapper invoiceMapper;
    
    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("50000"); // 最小经营规模阈值
    private static final BigDecimal MAX_CV_THRESHOLD = new BigDecimal("0.60"); // 最大波动系数阈值
    private static final BigDecimal STABLE_CV_THRESHOLD = new BigDecimal("0.30"); // 稳定波动系数阈值
    private static final BigDecimal LOAN_RATE = new BigDecimal("0.10"); // 基础贷款比例 10%
    private static final BigDecimal STABLE_DISCOUNT = new BigDecimal("1.0"); // 稳定不打折
    private static final BigDecimal UNSTABLE_DISCOUNT = new BigDecimal("0.7"); // 不稳定打7折
    private static final BigDecimal MAX_LOAN_LIMIT = new BigDecimal("200000"); // 最大贷款额度
    private static final BigDecimal MIN_LOAN_LIMIT = new BigDecimal("10000"); // 最小贷款额度
    
    /**
     * 指标一：计算经营规模指标
     * 近1-12个月(不含当月)销项发票价税合计总额
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期
     * @return 指标一结果
     */
    public Indicator1Result calculateIndicator1(String taxNo, String uscCode, LocalDate appDate) {
        // 筛选符合条件的发票
        List<TeachInvoice> validInvoices = filterValidInvoices(taxNo, uscCode, appDate);
        
        // 计算总金额
        BigDecimal totalAmount = validInvoices.stream()
                .map(TeachInvoice::getJshj)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // 四舍五入保留5位小数
        totalAmount = IndicatorStatUtil.roundToFiveDecimals(totalAmount);
        
        Indicator1Result result = new Indicator1Result();
        result.setSaleJshjSum(totalAmount);
        return result;
    }
    
    /**
     * 指标二：计算经营稳定性指标
     * 近1-12个月(不含当月)销项发票月度金额波动系数
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期
     * @return 指标二结果
     */
    public Indicator2Result calculateIndicator2(String taxNo, String uscCode, LocalDate appDate) {
        // 筛选符合条件的发票
        List<TeachInvoice> validInvoices = filterValidInvoices(taxNo, uscCode, appDate);
        
        // 按月份分组汇总
        Map<String, BigDecimal> monthAmounts = validInvoices.stream()
                .collect(Collectors.groupingBy(
                        invoice -> {
                            LocalDate invoiceDate = invoice.getInvoiceTime().toLocalDate();
                            YearMonth yearMonth = YearMonth.from(invoiceDate);
                            return yearMonth.toString(); // yyyy-MM
                        },
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                TeachInvoice::getJshj,
                                BigDecimal::add
                        )
                ));
        
        // 转换为列表用于计算统计指标
        List<BigDecimal> monthlyAmounts = monthAmounts.values().stream()
                .collect(Collectors.toList());
        
        // 计算均值
        BigDecimal meanAmt = IndicatorStatUtil.calculateMean(monthlyAmounts);
        
        // 计算标准差
        BigDecimal stdAmt = IndicatorStatUtil.calculateStdDev(monthlyAmounts, meanAmt);
        
        // 计算波动系数（变异系数）
        BigDecimal cv = IndicatorStatUtil.calculateCoefficientOfVariation(stdAmt, meanAmt);
        
        Indicator2Result result = new Indicator2Result();
        result.setMeanAmt(meanAmt);
        result.setStdAmt(stdAmt);
        result.setCv(cv);
        return result;
    }
    
    /**
     * 指标三：计算贷款决策
     * 基于指标一和指标二的结果，计算贷款额度和决策
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期
     * @return 指标三结果
     */
    public Indicator3Result calculateIndicator3(String taxNo, String uscCode, LocalDate appDate) {
        // 先计算指标一和指标二
        Indicator1Result indicator1 = calculateIndicator1(taxNo, uscCode, appDate);
        Indicator2Result indicator2 = calculateIndicator2(taxNo, uscCode, appDate);
        
        BigDecimal saleJshjSum = indicator1.getSaleJshjSum();
        BigDecimal cv = indicator2.getCv();
        
        Indicator3Result result = new Indicator3Result();
        Map<String, Object> features = new HashMap<>();
        features.put("sale_jshj_sum", saleJshjSum);
        features.put("mean_amt", indicator2.getMeanAmt());
        features.put("std_amt", indicator2.getStdAmt());
        features.put("cv", cv);
        result.setFeatures(features);
        
        // 决策规则1：经营规模不足
        if (saleJshjSum.compareTo(MIN_LOAN_AMOUNT) < 0) {
            result.setIsLoanable(false);
            result.setLoanLimit(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            result.setReason("经营规模不足，近12个月销项发票总额低于50,000元");
            return result;
        }
        
        // 决策规则2：波动过大
        if (cv.compareTo(MAX_CV_THRESHOLD) > 0) {
            result.setIsLoanable(false);
            result.setLoanLimit(BigDecimal.ZERO.setScale(5, RoundingMode.HALF_UP));
            result.setReason("经营波动过大，稳定性不足，波动系数超过0.60");
            return result;
        }
        
        // 可贷情况：计算贷款额度
        result.setIsLoanable(true);
        
        // 基础额度 = 销项总额 × 10%
        BigDecimal baseLimit = saleJshjSum.multiply(LOAN_RATE);
        
        // 稳定性调整系数
        BigDecimal discountRate;
        if (cv.compareTo(STABLE_CV_THRESHOLD) <= 0) {
            // cv ≤ 0.30：不打折
            discountRate = STABLE_DISCOUNT;
        } else {
            // 0.30 < cv ≤ 0.60：打7折
            discountRate = UNSTABLE_DISCOUNT;
        }
        
        // 最终额度 = 基础额度 × 稳定性系数
        BigDecimal finalLimit = baseLimit.multiply(discountRate);
        
        // 额度上限控制
        if (finalLimit.compareTo(MAX_LOAN_LIMIT) > 0) {
            finalLimit = MAX_LOAN_LIMIT;
        }
        
        // 额度下限控制
        if (finalLimit.compareTo(MIN_LOAN_LIMIT) < 0) {
            finalLimit = MIN_LOAN_LIMIT;
        }
        
        // 四舍五入保留5位小数
        finalLimit = finalLimit.setScale(5, RoundingMode.HALF_UP);
        
        result.setLoanLimit(finalLimit);
        result.setReason("企业近12个月经营规模达标，且经营稳定性良好");
        
        return result;
    }
    
    /**
     * 筛选符合条件的发票
     * 条件：sign='销项', state='正常', invoice_time 在 appDate 前1-12个月（不含当月）
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期
     * @return 符合条件的发票列表
     */
    private List<TeachInvoice> filterValidInvoices(String taxNo, String uscCode, LocalDate appDate) {
        LambdaQueryWrapper<TeachInvoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeachInvoice::getTaxNo, taxNo)
                .eq(TeachInvoice::getTaxpayerId, uscCode)
                .eq(TeachInvoice::getSign, "销项")
                .eq(TeachInvoice::getState, "正常")
                .eq(TeachInvoice::getDeleted, 0);
        
        // 计算时间范围：appDate 前1-12个月（不含当月）
        LocalDate startDate = appDate.minusMonths(12).withDayOfMonth(1); // 12个月前月初
        LocalDate endDate = appDate.minusMonths(1).withDayOfMonth(1); // 1个月前月初（不含当月）
        
        // 注意：invoice_time 是 DATETIME 类型，需要转换为 LocalDate 进行比较
        wrapper.ge(TeachInvoice::getInvoiceTime, startDate.atStartOfDay())
                .lt(TeachInvoice::getInvoiceTime, endDate.plusMonths(1).atStartOfDay()); // 小于下个月月初
        
        return invoiceMapper.selectList(wrapper);
    }
}

package com.spider.exc.controller;

import com.spider.exc.dto.Indicator1Result;
import com.spider.exc.dto.Indicator2Result;
import com.spider.exc.dto.Indicator3Result;
import com.spider.exc.service.IndicatorCalculateService;
import com.spider.exc.util.IndicatorDateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 指标计算接口
 * 提供三个核心指标的REST API
 */
@RestController
@RequestMapping("/api/indicator/calculate")
public class IndicatorCalculateController {
    
    @Autowired
    private IndicatorCalculateService calculateService;
    
    /**
     * 指标一：计算经营规模指标
     * 近1-12个月(不含当月)销项发票价税合计总额
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期（格式：yyyy-MM-dd）
     * @return 指标一结果
     */
    @GetMapping("/indicator1")
    public Indicator1Result calculateIndicator1(
            @RequestParam String taxNo,
            @RequestParam String uscCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appDate) {
        return calculateService.calculateIndicator1(taxNo, uscCode, appDate);
    }
    
    /**
     * 指标二：计算经营稳定性指标
     * 近1-12个月(不含当月)销项发票月度金额波动系数
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期（格式：yyyy-MM-dd）
     * @return 指标二结果
     */
    @GetMapping("/indicator2")
    public Indicator2Result calculateIndicator2(
            @RequestParam String taxNo,
            @RequestParam String uscCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appDate) {
        return calculateService.calculateIndicator2(taxNo, uscCode, appDate);
    }
    
    /**
     * 指标三：计算贷款决策
     * 基于指标一和指标二的结果，计算贷款额度和决策
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期（格式：yyyy-MM-dd）
     * @return 指标三结果
     */
    @GetMapping("/indicator3")
    public Indicator3Result calculateIndicator3(
            @RequestParam String taxNo,
            @RequestParam String uscCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appDate) {
        return calculateService.calculateIndicator3(taxNo, uscCode, appDate);
    }
    
    /**
     * 一次性计算所有三个指标
     * 
     * @param taxNo 税号
     * @param uscCode 统一社会信用代码
     * @param appDate 申请日期（格式：yyyy-MM-dd）
     * @return 包含三个指标结果的Map
     */
    @GetMapping("/all")
    public java.util.Map<String, Object> calculateAll(
            @RequestParam String taxNo,
            @RequestParam String uscCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appDate) {
        Indicator1Result indicator1 = calculateService.calculateIndicator1(taxNo, uscCode, appDate);
        Indicator2Result indicator2 = calculateService.calculateIndicator2(taxNo, uscCode, appDate);
        Indicator3Result indicator3 = calculateService.calculateIndicator3(taxNo, uscCode, appDate);
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("indicator1", indicator1);
        result.put("indicator2", indicator2);
        result.put("indicator3", indicator3);
        return result;
    }
}

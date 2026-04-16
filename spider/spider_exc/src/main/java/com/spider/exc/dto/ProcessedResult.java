package com.spider.exc.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 处理结果数据
 */
@Data
public class ProcessedResult {
    private String taxNo;
    private String uscCode;
    private String appDate;
    
    // 按月汇总的金额
    @JSONField(serialize = true)
    private Map<String, BigDecimal> monthSaleAmounts; // key: yyyy-MM, value: 该月销项发票金额总和
    
    // 统计指标
    private BigDecimal meanAmt; // 均值
    private BigDecimal stdAmt; // 标准差
    private BigDecimal cv; // 波动系数
    
    // 指标1的结果（经营规模指标）
    private BigDecimal totalSaleAmount; // 符合条件的销项发票总金额
}

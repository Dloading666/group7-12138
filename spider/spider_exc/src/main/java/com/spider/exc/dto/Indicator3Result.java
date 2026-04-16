package com.spider.exc.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 指标三：贷款决策结果
 * 基于两个指标计算贷款额度和决策
 */
@Data
public class Indicator3Result {
    /**
     * 是否可贷
     */
    private Boolean isLoanable;
    
    /**
     * 贷款额度
     */
    private BigDecimal loanLimit;
    
    /**
     * 决策原因
     */
    private String reason;
    
    /**
     * 特征值（包含指标一和指标二的结果）
     */
    private Map<String, Object> features;
    
    public Indicator3Result() {
        this.loanLimit = BigDecimal.ZERO.setScale(5);
    }
}

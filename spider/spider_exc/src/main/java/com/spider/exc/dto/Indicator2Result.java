package com.spider.exc.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 指标二：经营稳定性指标结果
 * 近1-12个月(不含当月)销项发票月度金额波动系数
 */
@Data
public class Indicator2Result {
    /**
     * 月均销项金额
     */
    private BigDecimal meanAmt;
    
    /**
     * 月度销项金额标准差
     */
    private BigDecimal stdAmt;
    
    /**
     * 变异系数（标准差/平均值）
     */
    private BigDecimal cv;
    
    /**
     * 说明
     */
    private String comment;
    
    public Indicator2Result() {
        this.comment = "近12个月销项金额波动系数，用于衡量经营稳定性";
    }
}

package com.spider.exc.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 指标一：经营规模指标结果
 * 近1-12个月(不含当月)销项发票价税合计总额
 */
@Data
public class Indicator1Result {
    /**
     * 近12个月销项发票价税合计总额
     */
    private BigDecimal saleJshjSum;
    
    /**
     * 说明
     */
    private String comment;
    
    public Indicator1Result() {
        this.comment = "近12个月销项发票价税合计总额，用于衡量企业经营规模";
    }
}

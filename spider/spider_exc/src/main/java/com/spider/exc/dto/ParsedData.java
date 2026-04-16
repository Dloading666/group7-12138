package com.spider.exc.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 解析后的结构化数据
 */
@Data
public class ParsedData {
    private String taxNo;
    private String uscCode;
    private String appDate;
    private List<ParsedInvoiceItem> invoices;
    
    @Data
    public static class ParsedInvoiceItem {
        private String sign;
        private String state;
        private String invoiceTime;
        private String jshj; // 原始字符串
        private String month; // 提取的月份 yyyy-MM
        private BigDecimal jshjDecimal; // 转换后的数值（去除千分位）
    }
}

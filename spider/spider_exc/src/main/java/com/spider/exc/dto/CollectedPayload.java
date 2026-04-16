package com.spider.exc.dto;

import lombok.Data;
import java.util.List;

/**
 * 采集的原始数据载荷
 */
@Data
public class CollectedPayload {
    private String taxNo;
    private String uscCode;
    private String appDate;
    private String enterpriseName; // 企业名称
    private List<InvoiceItem> invoices;
    
    @Data
    public static class InvoiceItem {
        private String sign; // 发票类型：销项/进项
        private String state; // 发票状态：正常/异常
        private String invoiceTime; // 开票时间
        private String jshj; // 价税合计（字符串，可能包含千分位）
    }
}

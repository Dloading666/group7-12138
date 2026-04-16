package com.spider.exc.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教学用发票信息表实体
 */
@Data
@TableName("teach_invoice")
public class TeachInvoice {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taxNo; // 税号
    
    private String taxpayerId; // 纳税人识别号（统一社会信用代码）
    
    private String sign; // 发票类型（销项/进项）
    
    private String state; // 发票状态（正常/作废/红冲）
    
    private LocalDateTime invoiceTime; // 发票开票时间
    
    private BigDecimal jshj; // 价税合计
    
    private String invoiceNo; // 发票号码
    
    private String invoiceCode; // 发票代码
    
    private Long createBy; // 创建人
    
    private LocalDateTime createTime; // 创建时间
    
    private Long updateBy; // 更新人
    
    private LocalDateTime updateTime; // 更新时间
    
    private Integer deleted; // 删除标志（0-未删除 1-已删除）
    
    private String remark; // 备注
}

package com.spider.exc.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标处理表实体
 */
@Data
@TableName("rpa_indicator_processing")
public class IndicatorProcessing {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long parsingId;
    
    private String taxNo;
    
    private String uscCode;
    
    private java.time.LocalDate appDate;
    
    private String processedResult; // JSON字符串
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

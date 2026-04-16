package com.spider.exc.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标采集表实体
 */
@Data
@TableName("rpa_indicator_collection")
public class IndicatorCollection {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String taxNo;
    
    private String uscCode;
    
    private java.time.LocalDate appDate;
    
    private String collectedPayload; // JSON字符串
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

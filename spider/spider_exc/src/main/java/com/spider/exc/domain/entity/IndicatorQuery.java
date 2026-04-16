package com.spider.exc.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 指标查询表实体
 */
@Data
@TableName("rpa_indicator_query")
public class IndicatorQuery {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long collectionId;
    
    private Long parsingId;
    
    private Long processingId;
    
    private String taxNo;
    
    private String uscCode;
    
    private java.time.LocalDate appDate;
    
    private String businessJson; // JSON字符串
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}

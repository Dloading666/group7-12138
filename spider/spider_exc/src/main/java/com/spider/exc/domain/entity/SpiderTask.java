package com.spider.exc.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 外部任务调度实体（对应 rpa_spider_task 表）
 */
@Data
@TableName("rpa_spider_task")
public class SpiderTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * project-gl 侧的任务ID
     */
    private String taskId;

    /**
     * 纳税人识别号
     */
    private String taxNo;

    /**
     * 统一社会信用代码
     */
    private String uscCode;

    /**
     * 申请日期
     */
    private LocalDate appDate;

    /**
     * 任务状态：pending / running / completed / failed
     */
    private String status;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 关联采集记录ID
     */
    private Long collectionId;

    /**
     * 关联解析记录ID
     */
    private Long parsingId;

    /**
     * 关联处理记录ID
     */
    private Long processingId;

    /**
     * 关联查询记录ID
     */
    private Long queryId;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

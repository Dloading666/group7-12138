package com.spider.exc.dto;

import lombok.Data;

/**
 * 接收 project-gl 提交的任务请求
 */
@Data
public class SpiderTaskRequest {

    /**
     * project-gl 侧的任务ID（唯一标识）
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
     * 申请日期（yyyy-MM-dd）
     */
    private String appDate;

    /**
     * project-gl 回调地址
     */
    private String callbackUrl;
}

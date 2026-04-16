package com.spider.exc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * spider_exc 返回给 project-gl 的响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpiderTaskResponse {

    private String taskId;
    private String status;
    private String message;
    private Object result;

    public static SpiderTaskResponse success(String taskId, String status, Object result) {
        return new SpiderTaskResponse(taskId, status, "success", result);
    }

    public static SpiderTaskResponse success(String taskId, String status) {
        return success(taskId, status, null);
    }

    public static SpiderTaskResponse failed(String taskId, String message) {
        return new SpiderTaskResponse(taskId, "failed", message, null);
    }
}

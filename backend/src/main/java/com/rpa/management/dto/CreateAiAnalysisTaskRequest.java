package com.rpa.management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAiAnalysisTaskRequest {

    @NotNull(message = "请选择来源采集结果")
    private Long sourceTaskRecordId;

    @NotNull(message = "请选择分析流程")
    private Long workflowId;

    @NotNull(message = "请选择执行机器人")
    private Long robotId;

    private String query;
}

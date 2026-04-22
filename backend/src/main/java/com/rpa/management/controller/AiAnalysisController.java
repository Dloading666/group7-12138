package com.rpa.management.controller;

import com.rpa.management.dto.AiAnalysisMessageDTO;
import com.rpa.management.dto.AiAnalysisQuestionRequest;
import com.rpa.management.dto.ApiResponse;
import com.rpa.management.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai-analysis")
@Tag(name = "历史 AI 分析", description = "兼容历史 ai_workflow 数据的只读接口")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/tasks")
    @Operation(summary = "已废弃：不再创建独立 AI 分析任务")
    public ApiResponse<Void> createTask() {
        return ApiResponse.error(410, "独立 AI 分析任务已下线，请改为在已完成任务运行详情中使用内置分析。");
    }

    @GetMapping("/tasks/{id}/messages")
    @Operation(summary = "读取历史 AI 分析问答记录")
    public ApiResponse<List<AiAnalysisMessageDTO>> getMessages(@PathVariable Long id) {
        try {
            return ApiResponse.success(aiAnalysisService.getMessages(id));
        } catch (Exception ex) {
            log.error("Failed to load AI analysis messages for {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PostMapping("/tasks/{id}/messages")
    @Operation(summary = "已废弃：历史 AI 分析记录只读")
    public ApiResponse<Void> askQuestion(@PathVariable Long id,
                                         @RequestBody(required = false) AiAnalysisQuestionRequest request) {
        log.info("Rejected deprecated ai-analysis follow-up for legacy task {}", id);
        return ApiResponse.error(410, "历史 AI 分析记录已切换为只读，请在任务运行详情中发起新的分析对话。");
    }
}

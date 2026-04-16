package com.rpa.management.controller;

import com.rpa.management.dto.AiAnalysisMessageDTO;
import com.rpa.management.dto.AiAnalysisQuestionRequest;
import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.CreateAiAnalysisTaskRequest;
import com.rpa.management.dto.TaskDTO;
import com.rpa.management.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
@Tag(name = "AI 分析", description = "AI 分析任务与问答接口")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/tasks")
    @Operation(summary = "创建 AI 分析任务")
    public ApiResponse<TaskDTO> createTask(@Valid @RequestBody CreateAiAnalysisTaskRequest request,
                                           HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            String userName = (String) httpRequest.getAttribute("username");
            return ApiResponse.success("AI 分析任务创建成功", aiAnalysisService.createAnalysisTask(request, userId, userName));
        } catch (Exception ex) {
            log.error("Failed to create AI analysis task", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @GetMapping("/tasks/{id}/messages")
    @Operation(summary = "获取 AI 分析问答记录")
    public ApiResponse<List<AiAnalysisMessageDTO>> getMessages(@PathVariable Long id) {
        try {
            return ApiResponse.success(aiAnalysisService.getMessages(id));
        } catch (Exception ex) {
            log.error("Failed to load AI analysis messages for {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }

    @PostMapping("/tasks/{id}/messages")
    @Operation(summary = "发送 AI 追问")
    public ApiResponse<List<AiAnalysisMessageDTO>> askQuestion(@PathVariable Long id,
                                                               @Valid @RequestBody AiAnalysisQuestionRequest request) {
        try {
            return ApiResponse.success("问答成功", aiAnalysisService.askQuestion(id, request.getQuestion()));
        } catch (Exception ex) {
            log.error("Failed to ask AI question for {}", id, ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }
}

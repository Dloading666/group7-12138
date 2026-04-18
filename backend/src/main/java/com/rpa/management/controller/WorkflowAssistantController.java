package com.rpa.management.controller;

import com.rpa.management.dto.ApiResponse;
import com.rpa.management.dto.WorkflowAssistantDraftRequest;
import com.rpa.management.service.WorkflowAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow-assistant")
@Tag(name = "流程助手", description = "AI 生成流程草稿接口")
public class WorkflowAssistantController {

    private final WorkflowAssistantService workflowAssistantService;

    @PostMapping("/drafts")
    @Operation(summary = "AI 生成流程草稿")
    public ApiResponse<Map<String, Object>> createDraft(@RequestBody WorkflowAssistantDraftRequest request) {
        try {
            return ApiResponse.success(workflowAssistantService.createDraft(request));
        } catch (Exception ex) {
            log.error("Failed to create workflow draft", ex);
            return ApiResponse.error(400, ex.getMessage());
        }
    }
}

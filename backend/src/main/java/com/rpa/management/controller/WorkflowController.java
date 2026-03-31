package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.PageResponse;
import com.rpa.management.common.PaginationUtils;
import com.rpa.management.dto.WorkflowDto;
import com.rpa.management.dto.WorkflowSaveRequest;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).WORKFLOW_VIEW)")
    public ApiResponse<PageResponse<WorkflowDto>> list(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(PaginationUtils.page(workflowService.listAll(), page, size));
    }

    @GetMapping("/design/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).WORKFLOW_VIEW)")
    public ApiResponse<WorkflowDto> detail(@PathVariable Long id) {
        return ApiResponse.success(workflowService.getById(id));
    }

    @PostMapping("/design")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).WORKFLOW_DESIGN)")
    public ApiResponse<WorkflowDto> create(@Valid @RequestBody WorkflowSaveRequest request) {
        return ApiResponse.success(workflowService.create(request));
    }

    @PutMapping("/design/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).WORKFLOW_DESIGN)")
    public ApiResponse<WorkflowDto> update(@PathVariable Long id, @Valid @RequestBody WorkflowSaveRequest request) {
        return ApiResponse.success(workflowService.update(id, request));
    }

    @DeleteMapping("/design/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).WORKFLOW_DESIGN)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return ApiResponse.success(null);
    }
}

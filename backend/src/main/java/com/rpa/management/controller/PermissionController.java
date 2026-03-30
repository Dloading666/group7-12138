package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.dto.PermissionNodeDto;
import com.rpa.management.dto.PermissionStatusRequest;
import com.rpa.management.dto.PermissionUpsertRequest;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_VIEW)")
    public ApiResponse<List<PermissionNodeDto>> tree() {
        return ApiResponse.success(permissionService.tree());
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_VIEW)")
    public ApiResponse<List<PermissionNodeDto>> list() {
        return ApiResponse.success(permissionService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_VIEW)")
    public ApiResponse<PermissionNodeDto> detail(@PathVariable Long id) {
        return ApiResponse.success(permissionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_CREATE)")
    public ApiResponse<PermissionNodeDto> create(@Valid @RequestBody PermissionUpsertRequest request) {
        return ApiResponse.success(permissionService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_UPDATE)")
    public ApiResponse<PermissionNodeDto> update(@PathVariable Long id, @Valid @RequestBody PermissionUpsertRequest request) {
        return ApiResponse.success(permissionService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_STATUS)")
    public ApiResponse<PermissionNodeDto> updateStatus(@PathVariable Long id, @Valid @RequestBody PermissionStatusRequest request) {
        return ApiResponse.success(permissionService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).PERMISSION_DELETE)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.success("OK", null);
    }
}

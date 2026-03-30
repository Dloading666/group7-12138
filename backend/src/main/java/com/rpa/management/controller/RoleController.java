package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.PaginationUtils;
import com.rpa.management.common.PageResponse;
import com.rpa.management.dto.RoleDto;
import com.rpa.management.dto.RolePermissionAssignmentDto;
import com.rpa.management.dto.RolePermissionUpdateRequest;
import com.rpa.management.dto.RoleStatusRequest;
import com.rpa.management.dto.RoleUpsertRequest;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_VIEW)")
    public ApiResponse<PageResponse<RoleDto>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(PaginationUtils.page(roleService.listAll(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_VIEW)")
    public ApiResponse<RoleDto> detail(@PathVariable Long id) {
        return ApiResponse.success(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_CREATE)")
    public ApiResponse<RoleDto> create(@Valid @RequestBody RoleUpsertRequest request) {
        return ApiResponse.success(roleService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_UPDATE)")
    public ApiResponse<RoleDto> update(@PathVariable Long id, @Valid @RequestBody RoleUpsertRequest request) {
        return ApiResponse.success(roleService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_UPDATE)")
    public ApiResponse<RoleDto> updateStatus(@PathVariable Long id, @Valid @RequestBody RoleStatusRequest request) {
        return ApiResponse.success(roleService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_DELETE)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.success("OK", null);
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_VIEW)")
    public ApiResponse<RolePermissionAssignmentDto> getPermissions(@PathVariable Long id) {
        return ApiResponse.success(roleService.getPermissions(id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).ROLE_ASSIGN_PERMISSIONS)")
    public ApiResponse<RolePermissionAssignmentDto> updatePermissions(@PathVariable Long id,
                                                                      @RequestBody RolePermissionUpdateRequest request) {
        return ApiResponse.success(roleService.updatePermissions(id, request));
    }
}

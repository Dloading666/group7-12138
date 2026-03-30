package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.common.PaginationUtils;
import com.rpa.management.common.PageResponse;
import com.rpa.management.dto.PermissionScopeDto;
import com.rpa.management.dto.UserDto;
import com.rpa.management.dto.UserPasswordRequest;
import com.rpa.management.dto.UserPermissionOverrideRequest;
import com.rpa.management.dto.UserStatusRequest;
import com.rpa.management.dto.UserUpsertRequest;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.UserService;
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

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_VIEW)")
    public ApiResponse<PageResponse<UserDto>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(PaginationUtils.page(userService.listAll(), page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_VIEW)")
    public ApiResponse<UserDto> detail(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_CREATE)")
    public ApiResponse<UserDto> create(@Valid @RequestBody UserUpsertRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_UPDATE)")
    public ApiResponse<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserUpsertRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_DELETE)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("OK", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_STATUS)")
    public ApiResponse<UserDto> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return ApiResponse.success(userService.updateStatus(id, request));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_RESET_PASSWORD)")
    public ApiResponse<UserDto> updatePassword(@PathVariable Long id, @Valid @RequestBody UserPasswordRequest request) {
        return ApiResponse.success(userService.updatePassword(id, request));
    }

    @GetMapping("/{id}/permissions/effective")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_ASSIGN_SCOPE)")
    public ApiResponse<PermissionScopeDto> getEffectivePermissions(@PathVariable Long id) {
        return ApiResponse.success(userService.getEffectivePermissions(id));
    }

    @GetMapping("/{id}/permissions/overrides")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_ASSIGN_SCOPE)")
    public ApiResponse<PermissionScopeDto> getOverrides(@PathVariable Long id) {
        return ApiResponse.success(userService.getPermissionOverrides(id));
    }

    @PutMapping("/{id}/permissions/overrides")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).USER_ASSIGN_SCOPE)")
    public ApiResponse<PermissionScopeDto> replaceOverrides(@PathVariable Long id,
                                                            @RequestBody UserPermissionOverrideRequest request) {
        return ApiResponse.success(userService.replacePermissionOverrides(id, request));
    }
}

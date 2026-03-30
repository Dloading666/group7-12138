package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.dto.UserDto;
import com.rpa.management.dto.UserPasswordRequest;
import com.rpa.management.security.UserPrincipal;
import com.rpa.management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserDto> profile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getById(principal.id()));
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserDto> password(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody UserPasswordRequest request) {
        return ApiResponse.success(userService.updatePassword(principal.id(), request));
    }
}

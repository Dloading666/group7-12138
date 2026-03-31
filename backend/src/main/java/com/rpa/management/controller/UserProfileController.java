package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.dto.ChangePasswordRequest;
import com.rpa.management.dto.UserDto;
import com.rpa.management.dto.UserProfileUpdateRequest;
import com.rpa.management.security.UserPrincipal;
import com.rpa.management.service.AvatarStorageService;
import com.rpa.management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/user")
public class UserProfileController {

    private final UserService userService;
    private final AvatarStorageService avatarStorageService;

    public UserProfileController(UserService userService, AvatarStorageService avatarStorageService) {
        this.userService = userService;
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserDto> profile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userService.getById(principal.id()));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserDto> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateProfile(principal.id(), request));
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserDto> password(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.success(userService.changeOwnPassword(principal.id(), request));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserDto> uploadAvatar(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam("file") MultipartFile file) {
        UserDto current = userService.getById(principal.id());
        String filename = avatarStorageService.store(file);
        String avatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/user/avatar/")
            .path(filename)
            .toUriString();
        UserDto updated = userService.updateAvatar(principal.id(), avatarUrl);
        avatarStorageService.deleteManagedAvatar(current.avatar());
        return ApiResponse.success(updated);
    }

    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable String filename) {
        Resource resource = avatarStorageService.load(filename);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, avatarStorageService.probeContentType(filename))
            .body(resource);
    }
}

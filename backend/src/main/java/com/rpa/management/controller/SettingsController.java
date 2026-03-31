package com.rpa.management.controller;

import com.rpa.management.common.ApiResponse;
import com.rpa.management.dto.BasicSettingsDto;
import com.rpa.management.dto.NotificationSettingsDto;
import com.rpa.management.security.PermissionCodes;
import com.rpa.management.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SystemSettingService settingService;

    public SettingsController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping("/basic")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).SETTINGS_BASIC)")
    public ApiResponse<BasicSettingsDto> getBasic() {
        return ApiResponse.success(settingService.getBasicSettings());
    }

    @PutMapping("/basic")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).SETTINGS_BASIC)")
    public ApiResponse<BasicSettingsDto> updateBasic(@Valid @RequestBody BasicSettingsDto request) {
        return ApiResponse.success(settingService.updateBasicSettings(request));
    }

    @GetMapping("/notification")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).SETTINGS_NOTIFICATION)")
    public ApiResponse<NotificationSettingsDto> getNotification() {
        return ApiResponse.success(settingService.getNotificationSettings());
    }

    @PutMapping("/notification")
    @PreAuthorize("hasAuthority(T(com.rpa.management.security.PermissionCodes).SETTINGS_NOTIFICATION)")
    public ApiResponse<NotificationSettingsDto> updateNotification(@Valid @RequestBody NotificationSettingsDto request) {
        return ApiResponse.success(settingService.updateNotificationSettings(request));
    }
}

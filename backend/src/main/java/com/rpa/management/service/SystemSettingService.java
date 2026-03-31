package com.rpa.management.service;

import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.dto.BasicSettingsDto;
import com.rpa.management.dto.NotificationSettingsDto;
import com.rpa.management.entity.SystemSetting;
import com.rpa.management.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    public static final String GROUP_BASIC = "basic";
    public static final String GROUP_NOTIFICATION = "notification";

    private static final Map<String, String> BASIC_DEFAULTS = Map.ofEntries(
        Map.entry("systemName", "RPA 管理系统"),
        Map.entry("systemSubtitle", "自动化任务与执行监控平台"),
        Map.entry("companyName", "RPA Platform"),
        Map.entry("supportEmail", "support@example.com"),
        Map.entry("supportPhone", "400-000-0000"),
        Map.entry("loginNotice", "请使用授权账号登录系统"),
        Map.entry("maintenanceMode", "false")
    );

    private static final Map<String, String> NOTIFICATION_DEFAULTS = Map.ofEntries(
        Map.entry("emailEnabled", "false"),
        Map.entry("emailHost", ""),
        Map.entry("emailPort", "587"),
        Map.entry("emailUsername", ""),
        Map.entry("emailFrom", "noreply@example.com"),
        Map.entry("webhookEnabled", "false"),
        Map.entry("webhookUrl", ""),
        Map.entry("taskFailureAlert", "true"),
        Map.entry("robotOfflineAlert", "true")
    );

    private final SystemSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public BasicSettingsDto getBasicSettings() {
        ensureDefaults(GROUP_BASIC, BASIC_DEFAULTS, "基础设置");
        Map<String, String> values = loadGroup(GROUP_BASIC);
        return new BasicSettingsDto(
            values.get("systemName"),
            values.get("systemSubtitle"),
            values.get("companyName"),
            values.get("supportEmail"),
            values.get("supportPhone"),
            values.get("loginNotice"),
            parseBoolean(values.get("maintenanceMode"))
        );
    }

    @Transactional
    public BasicSettingsDto updateBasicSettings(BasicSettingsDto request) {
        upsertGroup(GROUP_BASIC, Map.of(
            "systemName", safeValue(request.systemName()),
            "systemSubtitle", safeValue(request.systemSubtitle()),
            "companyName", safeValue(request.companyName()),
            "supportEmail", safeValue(request.supportEmail()),
            "supportPhone", safeValue(request.supportPhone()),
            "loginNotice", safeValue(request.loginNotice()),
            "maintenanceMode", String.valueOf(Boolean.TRUE.equals(request.maintenanceMode()))
        ), "基础设置");
        return getBasicSettings();
    }

    @Transactional(readOnly = true)
    public NotificationSettingsDto getNotificationSettings() {
        ensureDefaults(GROUP_NOTIFICATION, NOTIFICATION_DEFAULTS, "通知设置");
        Map<String, String> values = loadGroup(GROUP_NOTIFICATION);
        return new NotificationSettingsDto(
            parseBoolean(values.get("emailEnabled")),
            values.get("emailHost"),
            parseInteger(values.get("emailPort")),
            values.get("emailUsername"),
            values.get("emailFrom"),
            parseBoolean(values.get("webhookEnabled")),
            values.get("webhookUrl"),
            parseBoolean(values.get("taskFailureAlert")),
            parseBoolean(values.get("robotOfflineAlert"))
        );
    }

    @Transactional
    public NotificationSettingsDto updateNotificationSettings(NotificationSettingsDto request) {
        upsertGroup(GROUP_NOTIFICATION, Map.of(
            "emailEnabled", String.valueOf(Boolean.TRUE.equals(request.emailEnabled())),
            "emailHost", safeValue(request.emailHost()),
            "emailPort", String.valueOf(request.emailPort() == null ? 587 : request.emailPort()),
            "emailUsername", safeValue(request.emailUsername()),
            "emailFrom", safeValue(request.emailFrom()),
            "webhookEnabled", String.valueOf(Boolean.TRUE.equals(request.webhookEnabled())),
            "webhookUrl", safeValue(request.webhookUrl()),
            "taskFailureAlert", String.valueOf(Boolean.TRUE.equals(request.taskFailureAlert())),
            "robotOfflineAlert", String.valueOf(Boolean.TRUE.equals(request.robotOfflineAlert()))
        ), "通知设置");
        return getNotificationSettings();
    }

    private void ensureDefaults(String group, Map<String, String> defaults, String descriptionPrefix) {
        if (settingRepository.existsBySettingGroup(group)) {
            return;
        }
        List<SystemSetting> settings = defaults.entrySet().stream()
            .map(entry -> new SystemSetting()
                .setSettingGroup(group)
                .setSettingKey(entry.getKey())
                .setSettingValue(entry.getValue())
                .setDescription(descriptionPrefix + " - " + entry.getKey()))
            .toList();
        settingRepository.saveAll(settings);
    }

    private Map<String, String> loadGroup(String group) {
        Map<String, String> result = new LinkedHashMap<>();
        settingRepository.findAllBySettingGroupOrderBySettingKeyAsc(group)
            .forEach(setting -> result.put(setting.getSettingKey(), setting.getSettingValue()));
        if (GROUP_BASIC.equals(group)) {
            BASIC_DEFAULTS.forEach(result::putIfAbsent);
        } else if (GROUP_NOTIFICATION.equals(group)) {
            NOTIFICATION_DEFAULTS.forEach(result::putIfAbsent);
        }
        return result;
    }

    private void upsertGroup(String group, Map<String, String> values, String descriptionPrefix) {
        if (values.isEmpty()) {
            throw new BadRequestBusinessException("Settings payload is empty");
        }
        values.forEach((key, value) -> {
            SystemSetting setting = settingRepository.findBySettingGroupAndSettingKey(group, key)
                .orElseGet(SystemSetting::new);
            setting.setSettingGroup(group)
                .setSettingKey(key)
                .setSettingValue(value)
                .setDescription(descriptionPrefix + " - " + key);
            settingRepository.save(setting);
        });
    }

    private boolean parseBoolean(String value) {
        return value != null && Boolean.parseBoolean(value);
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String safeValue(String value) {
        return value == null ? "" : value.trim();
    }
}

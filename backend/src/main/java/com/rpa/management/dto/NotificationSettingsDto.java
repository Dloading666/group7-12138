package com.rpa.management.dto;

public record NotificationSettingsDto(
    Boolean emailEnabled,
    String emailHost,
    Integer emailPort,
    String emailUsername,
    String emailFrom,
    Boolean webhookEnabled,
    String webhookUrl,
    Boolean taskFailureAlert,
    Boolean robotOfflineAlert
) {
}

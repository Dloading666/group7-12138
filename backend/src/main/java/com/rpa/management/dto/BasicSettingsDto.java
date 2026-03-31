package com.rpa.management.dto;

public record BasicSettingsDto(
    String systemName,
    String systemSubtitle,
    String companyName,
    String supportEmail,
    String supportPhone,
    String loginNotice,
    Boolean maintenanceMode
) {
}

package com.rpa.management.dto;

import com.rpa.management.common.enums.PermissionStatus;
import jakarta.validation.constraints.NotNull;

public record PermissionStatusRequest(
    @NotNull PermissionStatus status
) {
}

package com.rpa.management.dto;

import com.rpa.management.common.enums.RoleStatus;
import jakarta.validation.constraints.NotNull;

public record RoleStatusRequest(
    @NotNull RoleStatus status
) {
}

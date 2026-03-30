package com.rpa.management.dto;

import com.rpa.management.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
    @NotNull UserStatus status
) {
}

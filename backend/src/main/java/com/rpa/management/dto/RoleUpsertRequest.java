package com.rpa.management.dto;

import com.rpa.management.common.enums.RoleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleUpsertRequest(
    @NotBlank String name,
    @NotBlank String code,
    String description,
    @NotNull RoleStatus status,
    Boolean builtIn
) {
}

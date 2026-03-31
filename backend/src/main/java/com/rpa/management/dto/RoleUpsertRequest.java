package com.rpa.management.dto;

import com.rpa.management.common.enums.RoleStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleUpsertRequest(
    @NotBlank String name,
    @JsonAlias("permissionCode")
    @NotBlank String code,
    String description,
    @NotNull RoleStatus status,
    Boolean builtIn
) {
}

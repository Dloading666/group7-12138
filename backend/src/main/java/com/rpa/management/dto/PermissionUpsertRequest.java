package com.rpa.management.dto;

import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.enums.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PermissionUpsertRequest(
    @NotBlank String name,
    @NotBlank String code,
    @NotNull PermissionType type,
    Long parentId,
    String path,
    String component,
    String icon,
    Integer sortOrder,
    @NotNull PermissionStatus status
) {
}

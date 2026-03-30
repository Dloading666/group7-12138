package com.rpa.management.dto;

import java.util.List;

public record PermissionScopeDto(
    Long userId,
    List<Long> rolePermissionIds,
    List<Long> grantedPermissionIds,
    List<Long> revokedPermissionIds,
    List<Long> effectivePermissionIds,
    List<String> effectivePermissionCodes,
    List<PermissionNodeDto> menuTree
) {
}

package com.rpa.management.dto;

import java.util.List;

public record RolePermissionAssignmentDto(
    Long roleId,
    List<Long> permissionIds,
    List<PermissionNodeDto> tree
) {
}

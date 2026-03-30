package com.rpa.management.dto;

import java.util.List;

public record RolePermissionUpdateRequest(
    List<Long> permissionIds
) {
}

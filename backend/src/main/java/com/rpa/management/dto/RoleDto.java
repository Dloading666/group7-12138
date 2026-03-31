package com.rpa.management.dto;

import com.rpa.management.common.enums.RoleStatus;
import com.rpa.management.entity.Role;

public record RoleDto(
    Long id,
    String name,
    String code,
    String permissionCode,
    String description,
    RoleStatus status,
    boolean builtIn
) {
    public static RoleDto from(Role role) {
        return new RoleDto(
            role.getId(),
            role.getName(),
            role.getCode(),
            role.getCode(),
            role.getDescription(),
            role.getStatus(),
            role.isBuiltIn()
        );
    }
}

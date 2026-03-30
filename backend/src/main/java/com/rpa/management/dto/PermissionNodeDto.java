package com.rpa.management.dto;

import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.enums.PermissionType;
import com.rpa.management.entity.Permission;

import java.util.List;

public record PermissionNodeDto(
    Long id,
    String name,
    String code,
    PermissionType type,
    Long parentId,
    String path,
    String component,
    String icon,
    Integer sortOrder,
    PermissionStatus status,
    List<PermissionNodeDto> children
) {
    public static PermissionNodeDto from(Permission permission, List<PermissionNodeDto> children) {
        return new PermissionNodeDto(
            permission.getId(),
            permission.getName(),
            permission.getCode(),
            permission.getType(),
            permission.getParentId(),
            permission.getPath(),
            permission.getComponent(),
            permission.getIcon(),
            permission.getSortOrder(),
            permission.getStatus(),
            children
        );
    }
}

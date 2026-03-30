package com.rpa.management.dto;

import java.util.List;

public record LoginResponse(
    String accessToken,
    String token,
    String tokenType,
    Long expiresIn,
    UserDto user,
    RoleDto role,
    List<String> permissionCodes,
    List<PermissionNodeDto> menuTree
) {
}

package com.rpa.management.security;

import java.io.Serializable;
import java.util.Collection;

public record UserPrincipal(
    Long id,
    String username,
    String realName,
    String roleCode,
    boolean superAdmin,
    Collection<String> permissionCodes
) implements Serializable {
}

package com.rpa.management.dto;

import java.util.List;

public record UserPermissionOverrideRequest(
    List<Long> grants,
    List<Long> revokes
) {
}

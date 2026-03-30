package com.rpa.management.dto;

import com.rpa.management.common.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserUpsertRequest(
    @NotBlank String username,
    String password,
    @NotBlank String realName,
    String email,
    String phone,
    String avatar,
    @NotNull Long roleId,
    @NotNull UserStatus status,
    Boolean superAdmin
) {
}

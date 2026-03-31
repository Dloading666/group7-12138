package com.rpa.management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
    @NotBlank @Size(max = 64) String realName,
    @Email @Size(max = 128) String email,
    @Size(max = 32) String phone,
    @Size(max = 255) String avatar
) {
}

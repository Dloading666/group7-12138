package com.rpa.management.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordRequest(
    @NotBlank String password
) {
}

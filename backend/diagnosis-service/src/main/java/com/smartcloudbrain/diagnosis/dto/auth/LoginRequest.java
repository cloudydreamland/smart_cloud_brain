package com.smartcloudbrain.diagnosis.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String account,
    @NotBlank String password
) {
}

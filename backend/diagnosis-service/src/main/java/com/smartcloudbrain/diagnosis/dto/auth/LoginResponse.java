package com.smartcloudbrain.diagnosis.dto.auth;

public record LoginResponse(
    String token,
    Long userId,
    String role,
    String name
) {
}

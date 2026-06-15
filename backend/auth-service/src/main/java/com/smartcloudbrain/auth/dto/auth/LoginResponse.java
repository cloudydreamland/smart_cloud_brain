package com.smartcloudbrain.auth.dto.auth;

public record LoginResponse(
    String token,
    Long userId,
    String role,
    String name
) {
}



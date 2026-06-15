package com.smartcloudbrain.common.security;

import java.time.Instant;

public record JwtClaims(Long userId, RoleType role, String name, Instant expiresAt) {
}

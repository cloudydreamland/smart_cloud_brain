package com.smartcloudbrain.common.security;

public record AuthenticatedUser(Long userId, RoleType role, String name) {
}

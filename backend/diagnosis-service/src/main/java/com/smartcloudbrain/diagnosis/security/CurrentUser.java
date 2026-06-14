package com.smartcloudbrain.diagnosis.security;

import com.smartcloudbrain.common.security.RoleType;

public record CurrentUser(
    Long userId,
    RoleType role,
    String name
) {
}

package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record RolePermissionSaveRequest(
    @NotBlank String role,
    List<String> permissionKeys
) {
}

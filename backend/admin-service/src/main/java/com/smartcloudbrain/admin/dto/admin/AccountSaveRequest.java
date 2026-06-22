package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AccountSaveRequest(
    Long id,
    @NotBlank String role,
    @NotBlank String account,
    @NotBlank String name,
    String password,
    Long departmentId,
    String title,
    String specialty,
    String status
) {}

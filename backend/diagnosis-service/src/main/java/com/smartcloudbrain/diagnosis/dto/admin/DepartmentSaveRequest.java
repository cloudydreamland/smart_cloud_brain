package com.smartcloudbrain.diagnosis.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record DepartmentSaveRequest(
    Long id,
    @NotBlank String code,
    @NotBlank String name,
    String description
) {
}

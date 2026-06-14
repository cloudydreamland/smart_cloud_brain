package com.smartcloudbrain.diagnosis.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DoctorSaveRequest(
    Long id,
    @NotBlank String name,
    @NotBlank String phone,
    String password,
    @NotNull Long departmentId,
    String title,
    String specialty,
    String status
) {
}

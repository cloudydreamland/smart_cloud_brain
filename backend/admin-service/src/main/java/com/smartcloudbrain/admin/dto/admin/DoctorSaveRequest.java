package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DoctorSaveRequest(
    Long id,
    @NotBlank String name,
    @NotBlank
    @Pattern(regexp = "\\d{11}", message = "手机号必须为11位数字")
    String phone,
    String password,
    @NotNull Long departmentId,
    String title,
    String specialty,
    String status
) {
}



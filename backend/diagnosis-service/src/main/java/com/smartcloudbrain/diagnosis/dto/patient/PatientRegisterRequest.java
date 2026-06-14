package com.smartcloudbrain.diagnosis.dto.patient;

import jakarta.validation.constraints.NotBlank;

public record PatientRegisterRequest(
    @NotBlank String name,
    @NotBlank String phone,
    @NotBlank String password,
    String gender,
    Integer age,
    String allergyHistory,
    String pastHistory
) {
}

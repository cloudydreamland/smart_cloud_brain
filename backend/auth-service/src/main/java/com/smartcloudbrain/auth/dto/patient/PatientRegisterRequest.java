package com.smartcloudbrain.auth.dto.patient;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record PatientRegisterRequest(
    @NotBlank String name,
    @NotBlank String phone,
    @NotBlank @Email String email,
    @NotBlank String emailCode,
    @NotBlank String password,
    String gender,
    Integer age,
    String allergyHistory,
    String pastHistory
) {
}



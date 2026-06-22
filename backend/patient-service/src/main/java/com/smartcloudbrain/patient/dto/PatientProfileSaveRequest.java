package com.smartcloudbrain.patient.dto;

import jakarta.validation.constraints.NotBlank;

public record PatientProfileSaveRequest(
    @NotBlank String name,
    String gender,
    Integer age,
    String allergyHistory,
    String pastHistory
) {
}

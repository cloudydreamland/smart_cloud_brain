package com.smartcloudbrain.patient.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record PatientVisitorSaveRequest(
    Long id,
    @NotBlank String name,
    String relationship,
    String phone,
    String gender,
    Integer age,
    String address,
    String emergencyContact,
    String emergencyPhone,
    String bloodType,
    Integer heightCm,
    BigDecimal weightKg,
    String allergyHistory,
    String pastHistory
) {
}

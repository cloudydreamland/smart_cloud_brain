package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record MedicalRecordGenerateRequest(
    Long registrationId,
    String departmentCode,
    @NotBlank String dialogueText
) {
}

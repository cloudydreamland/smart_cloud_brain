package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record TriageRequest(
    Long patientId,
    @NotBlank String chiefComplaint
) {
}

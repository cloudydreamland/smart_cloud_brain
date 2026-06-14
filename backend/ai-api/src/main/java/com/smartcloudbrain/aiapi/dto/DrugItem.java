package com.smartcloudbrain.aiapi.dto;

import jakarta.validation.constraints.NotBlank;

public record DrugItem(
    @NotBlank String drugName,
    @NotBlank String dosage,
    @NotBlank String frequency,
    @NotBlank String usageMethod
) {
}

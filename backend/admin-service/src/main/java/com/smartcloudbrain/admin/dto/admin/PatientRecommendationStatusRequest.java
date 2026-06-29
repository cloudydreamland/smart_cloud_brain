package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotNull;

public record PatientRecommendationStatusRequest(
    @NotNull Long id,
    String status
) {}

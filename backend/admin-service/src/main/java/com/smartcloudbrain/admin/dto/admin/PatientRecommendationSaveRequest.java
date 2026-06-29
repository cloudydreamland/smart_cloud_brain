package com.smartcloudbrain.admin.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PatientRecommendationSaveRequest(
    Long id,
    @NotBlank @Size(max = 32) String recommendType,
    @NotNull Long targetId,
    @Size(max = 120) String title,
    @Size(max = 500) String description,
    @Size(max = 500) String imageUrl,
    @Size(max = 500) String imageObjectKey,
    Integer sort,
    @Size(max = 20) String status
) {}
